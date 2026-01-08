package io.github.siyukio.tools.api;

import org.springframework.util.Assert;

public class ApiProperties {

    public static final String CONFIG_PREFIX = "spring.siyukio";

    /**
     * JWT security configuration.
     */
    private Jwt jwt = new Jwt();

    /**
     * API signature configuration.
     */
    private Signature signature = new Signature();

    /**
     * Profile and environment configuration.
     */
    private Profiles profiles = new Profiles();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        Assert.notNull(jwt, "JWT configuration must not be null");
        this.jwt = jwt;
    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        Assert.notNull(signature, "Signature configuration must not be null");
        this.signature = signature;
    }

    public Profiles getProfiles() {
        return profiles;
    }

    public void setProfiles(Profiles profiles) {
        Assert.notNull(profiles, "Profiles configuration must not be null");
        this.profiles = profiles;
    }

    /**
     * JWT security configuration properties.
     */
    public static class Jwt {

        /**
         * JWT public key for token validation.
         * <p>
         * Used to verify the authenticity of JWT tokens issued by the application.
         */
        private String publicKey = "";

        /**
         * JWT private key for token generation.
         * <p>
         * Used to sign JWT tokens for secure authentication.
         */
        private String privateKey = "";

        /**
         * JWT password for additional token security.
         * <p>
         * Provides an extra layer of security for JWT token operations.
         */
        private String password = "siyukio";

        /**
         * Duration for access token validity.
         * <p>
         * Defines how long the access token remains valid before expiration.
         * Default value is PT15M (15 minutes) in ISO-8601 duration format.
         */
        private String accessTokenDuration = "PT15M";

        /**
         * Duration for refresh token validity.
         * <p>
         * Defines how long the refresh token remains valid before expiration.
         * Default value is P30D (30 days) in ISO-8601 duration format.
         */
        private String refreshTokenDuration = "P30D";

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            Assert.hasText(publicKey, "JWT public key must not be empty");
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            Assert.hasText(privateKey, "JWT private key must not be empty");
            this.privateKey = privateKey;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            Assert.hasText(password, "JWT password must not be empty");
            this.password = password;
        }

        public String getAccessTokenDuration() {
            return accessTokenDuration;
        }

        public void setAccessTokenDuration(String accessTokenDuration) {
            Assert.hasText(accessTokenDuration, "Access token duration must not be empty");
            this.accessTokenDuration = accessTokenDuration;
        }

        public String getRefreshTokenDuration() {
            return refreshTokenDuration;
        }

        public void setRefreshTokenDuration(String refreshTokenDuration) {
            Assert.hasText(refreshTokenDuration, "Refresh token duration must not be empty");
            this.refreshTokenDuration = refreshTokenDuration;
        }
    }

    /**
     * API signature configuration properties.
     */
    public static class Signature {

        /**
         * Salt value for API signature generation.
         * <p>
         * Used to generate secure signatures for API request validation.
         */
        private String salt = "siyukio";

        public String getSalt() {
            return salt;
        }

        public void setSalt(String salt) {
            Assert.hasText(salt, "Signature salt must not be empty");
            this.salt = salt;
        }
    }

    /**
     * Profile and environment configuration properties.
     */
    public static class Profiles {

        /**
         * Flag to enable/disable API documentation.
         * <p>
         * When true, API documentation endpoints will be accessible.
         */
        private Boolean docs = false;

        /**
         * Active profile for the application.
         * <p>
         * Defines the current runtime environment (e.g., dev, test, prod).
         */
        private String active = "dev";

        public Boolean getDocs() {
            return docs;
        }

        public void setDocs(Boolean docs) {
            Assert.notNull(docs, "Profiles docs flag must not be null");
            this.docs = docs;
        }

        public String getActive() {
            return active;
        }

        public void setActive(String active) {
            Assert.hasText(active, "Profiles active must not be empty");
            this.active = active;
        }
    }
}
