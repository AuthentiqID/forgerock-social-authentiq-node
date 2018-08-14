/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */

package com.authentiq.openam.auth.nodes;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.DEFAULT_OAUTH2_SCOPE_DELIMITER;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode;
import org.forgerock.openam.auth.nodes.oauth.ProfileNormalizer;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.validation.URLValidator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The social authentiq node. Contains pre-populated configuration for authentiq.
 */
@Node.Metadata(outcomeProvider = AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class,
        configClass = SocialAuthentiqNode.AuthentiqOAuth2Config.class)
public class SocialAuthentiqNode extends AbstractSocialAuthLoginNode {

    public static final String AUTH_ID_KEY = "sub";
    public static final String ISSUER = "https://connect.authentiq.io/";
    public static final Boolean USE_BASIC_AUTH = true;

    /**
     * The node config with default values for authentiq.
     */
    public interface AuthentiqOAuth2Config extends AbstractSocialAuthLoginNode.Config {

        /**
         * the client id.
         * @return the client id
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        String clientId();

        /**
         * The client secret.
         * @return the client secret
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        @Password
        char[] clientSecret();

        /**
         * The authorization endpoint.
         * @return The authorization endpoint.
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class, URLValidator.class})
        default String authentiqEndpoint() {
            return "https://connect.authentiq.io/";
        }

        /**
         * The scopes to request.
         * @return the scopes.
         */
        @Attribute(order = 600, validators = {RequiredValueValidator.class})
        default String scopeString() {
            return "openid profile email~s phone~s address aq:push";
        }

        /**
         * The URI the AS will redirect to.
         * @return the redirect URI
         */
        @Attribute(order = 700, validators = {RequiredValueValidator.class, URLValidator.class})
        default String redirectURI() {
            return getServerURL();
        }

        /**
         * The provider. (useful if using IDM)
         * @return the provider.
         */
        @Attribute(order = 800)
        default String provider() {
            return "authentiq";
        }

        /**
         * The account provider class.
         * @return The account provider class.
         */
        @Attribute(order = 1100, validators = {RequiredValueValidator.class})
        default String cfgAccountProviderClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider";
        }

        /**
         * The account mapper class.
         * @return the account mapper class.
         */
        @Attribute(order = 1200, validators = {RequiredValueValidator.class})
        default String cfgAccountMapperClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.JsonAttributeMapper|*|authentiq-";
        }

        /**
         * The attribute mapping classes.
         * @return the attribute mapping classes.
         */
        @Attribute(order = 1300, validators = {RequiredValueValidator.class})
        default Set<String> cfgAttributeMappingClasses() {
            return singleton("org.forgerock.openam.authentication.modules.common.mapping."
                    + "JsonAttributeMapper|iplanet-am-user-alias-list|authentiq-");
        }

        /**
         * The account mapper configuration.
         * @return the account mapper configuration.
         */
        @Attribute(order = 1400, validators = {RequiredValueValidator.class})
        default Map<String, String> cfgAccountMapperConfiguration() {
            return singletonMap("sub", "iplanet-am-user-alias-list");
        }

        /**
         * The attribute mapping configuration.
         * @return the attribute mapping configuration
         */
        @Attribute(order = 1500, validators = {RequiredValueValidator.class})
        default Map<String, String> cfgAttributeMappingConfiguration() {
            final Map<String, String> attributeMappingConfiguration = new HashMap<>();
            attributeMappingConfiguration.put("sub", "iplanet-am-user-alias-list");
            attributeMappingConfiguration.put("given_name", "givenName");
            attributeMappingConfiguration.put("family_name", "sn");
            attributeMappingConfiguration.put("name", "cn");
            attributeMappingConfiguration.put("email", "mail");
            attributeMappingConfiguration.put("phone_number", "telephoneNumber");
            attributeMappingConfiguration.put("locale", "preferredlocale");
            attributeMappingConfiguration.put("zoneinfo", "preferredtimezone");
            return attributeMappingConfiguration;
        }

        /**
         * Specifies if the user attributes must be saved in session.
         * @return true to save the user attribute into the session, false otherwise.
         */
        @Attribute(order = 1600)
        default boolean saveUserAttributesToSession() {
            return true;
        }

        /**
         * Specify if the mixup mitigation must be activated.
         * The mixup mitigation add an extra level of security by checking the client_id and iss coming from the
         * authorizeEndpoint response.
         *
         * @return true to activate it , false otherwise
         */
        @Attribute(order = 1700)
        default boolean cfgMixUpMitigation() {
            return false;
        }

        /**
         * The issuer. Must be specify to use the mixup mitigation.
         * @return the issuer.
         */
        @Attribute(order = 1800)
        default String issuer() {
            return "https://connect.authentiq.io/";
        }
    }

    /**
     * Constructs a new {@link SocialAuthentiqNode} with the provided {@link Config}.
     *
     * @param config           provides the settings for initialising an {@link SocialAuthentiqNode}.
     * @param authModuleHelper helper for oauth2
     * @param profileNormalizer User profile normaliser
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public SocialAuthentiqNode(@Assisted AuthentiqOAuth2Config config, SocialOAuth2Helper authModuleHelper,
                               ProfileNormalizer profileNormalizer) throws NodeProcessException {
        super(config, authModuleHelper, authModuleHelper.newOAuthClient(getOAuthClientConfiguration(config)),
                profileNormalizer);
    }

    private static OAuthClientConfiguration getOAuthClientConfiguration(AuthentiqOAuth2Config config) throws NodeProcessException {

        URI baseURI;

        try {
            baseURI = new URI(config.authentiqEndpoint());
        } catch (URISyntaxException e) {
            throw new NodeProcessException("Invalid Authentiq base URL.", e);
        }

        return OAuth2ClientConfiguration.oauth2ClientConfiguration()
                .withClientId(config.clientId())
                .withClientSecret(new String(config.clientSecret()))
                .withAuthorizationEndpoint(baseURI.resolve("sign-in").toString())
                .withTokenEndpoint(baseURI.resolve("token").toString())
                .withScope(Collections.singletonList(config.scopeString()))
                .withScopeDelimiter(DEFAULT_OAUTH2_SCOPE_DELIMITER)
                .withBasicAuth(USE_BASIC_AUTH)
                .withUserInfoEndpoint(baseURI.resolve("userinfo").toString())
                .withRedirectUri(URI.create(config.redirectURI()))
                .withProvider(config.provider())
                .withAuthenticationIdKey(AUTH_ID_KEY).build();
    }
}
