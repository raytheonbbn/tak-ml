package com.atakmap.android.takml_android.configuration;

public class TakmlServerConfiguration {
    public static TakmlServerConfiguration DEFAULT_CONFIGURATION = new TakmlServerConfiguration.Builder().build();
    public static final int TAKML_SERVER_DEFAULT_PORT = 8234;

    /// Network
    private final boolean shareTakServerIpAndCerts;
    private final String ip;
    private final int port;
    private final String apiKeyName;
    private final String apiKey;
    private final byte[] clientStoreCert;
    private final byte[] trustStoreCert;
    private final String clientStorePass;
    private final String truststorePass;

    /// Components
    private final MetricsConfiguration metricsConfiguration;

    private TakmlServerConfiguration(TakmlServerConfiguration.Builder builder) {

        this.shareTakServerIpAndCerts = builder.shareTakServerIpAndCerts;
        this.ip = builder.ip;
        this.port = builder.port;
        this.apiKeyName = builder.apiKeyName;
        this.apiKey = builder.apiKey;
        this.clientStoreCert = builder.clientStoreCert;
        this.trustStoreCert = builder.trustStoreCert;
        this.clientStorePass = builder.clientStorePass;
        this.truststorePass = builder.truststorePass;
        this.metricsConfiguration = builder.metricsConfiguration;
    }

    public static class Builder{
        /// Network
        private boolean shareTakServerIpAndCerts = false;
        private String ip;
        private int port;
        private String apiKeyName;
        private String apiKey;
        private byte[] clientStoreCert;
        private byte[] trustStoreCert;
        private String clientStorePass;
        private String truststorePass;

        /// Components
        private MetricsConfiguration metricsConfiguration = MetricsConfiguration.DEFAULT_CONFIGURATION;

        /**
         * Use the default configuration: TAK Server and TAK ML Server share the same Ip/Port
         * and PKI certificates
         */
        public Builder(){
            this.shareTakServerIpAndCerts = true;
            this.port = TAKML_SERVER_DEFAULT_PORT;
        }

        public Builder(String ip, int port, String apiKeyName, String apiKey){
            this.ip = ip;
            this.port = port;
            this.apiKeyName = apiKeyName;
            this.apiKey = apiKey;
        }

        public Builder(String ip, int port, byte[] clientStoreCert, byte[] trustStoreCert,
                       String clientStorePass, String truststorePass) {
            this.ip = ip;
            this.port = port;
            this.clientStoreCert = clientStoreCert;
            this.trustStoreCert = trustStoreCert;
            this.clientStorePass = clientStorePass;
            this.truststorePass = truststorePass;
        }

        public Builder configureMetrics(MetricsConfiguration metricsConfiguration){
            this.metricsConfiguration = metricsConfiguration;
            return this;
        }

        public TakmlServerConfiguration build(){
            return new TakmlServerConfiguration(this);
        }
    }

    public boolean isShareTakServerIpAndCerts() {
        return shareTakServerIpAndCerts;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public byte[] getClientStoreCert() {
        return clientStoreCert;
    }

    public byte[] getTrustStoreCert() {
        return trustStoreCert;
    }

    public String getClientStorePass() {
        return clientStorePass;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }
}
