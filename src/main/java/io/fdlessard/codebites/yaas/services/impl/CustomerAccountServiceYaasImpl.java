package io.fdlessard.codebites.yaas.services.impl;

import com.sap.cloud.yaas.servicesdk.authorization.AccessToken;
import com.sap.cloud.yaas.servicesdk.authorization.AccessTokenRequestException;
import com.sap.cloud.yaas.servicesdk.authorization.AuthorizationScope;
import com.sap.cloud.yaas.servicesdk.authorization.DiagnosticContext;
import com.sap.cloud.yaas.servicesdk.authorization.integration.AuthorizedExecutionCallback;
import com.sap.cloud.yaas.servicesdk.authorization.integration.AuthorizedExecutionTemplate;
import io.fdlessard.codebites.yaas.domain.CustomerAccount;
import io.fdlessard.codebites.yaas.properties.CustomerAccountServiceProperties;
import io.fdlessard.codebites.yaas.services.CustomerAccountService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fdlessard on 16-10-27.
 */
@Service
public class CustomerAccountServiceYaasImpl implements CustomerAccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAccountServiceYaasImpl.class);

    @Autowired
    private CustomerAccountServiceProperties customerAccountServiceProperties;

    @Autowired
    private AuthorizedExecutionTemplate authorizedExecutionTemplate;

    @Autowired
    private RestOperations customerAccountServiceYaasRestTemplate;


    @Override
    public List<CustomerAccount> getCustomerAccounts() {

        LOGGER.debug("getCustomerAccounts()");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        List<CustomerAccount> response = null;

        try {

            response = authorizedExecutionTemplate.executeAuthorized(

                    new AuthorizationScope(customerAccountServiceProperties.getTenant(), customerAccountServiceProperties.getScopes()),
                    new DiagnosticContext("requestId", 0),
                    new AuthorizedExecutionCallback<List<CustomerAccount>>() {
                        @Override
                        public List<CustomerAccount> execute(final AccessToken token) {
                            // execute requests to other YaaS services with the given token in the "Authorization" header
                            // return Response object
                            String authorization = token.getType() + " " + token.getValue();
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, token.getType() + " " + token.getValue());
                            HttpEntity<String> requestEntity = new HttpEntity<>(StringUtils.EMPTY, httpHeaders);
                            ResponseEntity<List> response = null;
                            response = customerAccountServiceYaasRestTemplate.exchange(URI.create(buildUrl()), HttpMethod.GET, requestEntity, List.class);
                            return response.getBody();
                        }
                    });

        } catch (HttpClientErrorException | AccessTokenRequestException e) {
            LOGGER.error("Problem in CustomerAccountServiceYaasImpl()", e);
        }

        return response;
    }

    private String buildUrl() {
        return UriComponentsBuilder.fromUriString(customerAccountServiceProperties.getCustomerUrl()).buildAndExpand(customerAccountServiceProperties.getTenant()).toUriString();
    }

}
