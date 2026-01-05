package com.atakmap.android.takml_android.configuration;

public class MetricsConfiguration {
    public static final MetricsConfiguration DEFAULT_CONFIGURATION = new MetricsConfiguration(Mode.ONLINE);

    /**
     * Operation Mode
     *
     * <pre>
     *     ONLINE: publish if connected to TAK ML Server
     *     DISABLED: don't record metrics
     *     OFFLINE: record metrics locally only
     *     OFFLINE_PUSH_ON_RECONNECT: record metrics locally and publish on connection to TAK ML Server
     * </pre>
     */
    public enum Mode {ONLINE, DISABLED, OFFLINE_PUSH_ON_RECONNECT};

    private final Mode operationMode;
    private Long publishRateSeconds = 10L;

    /**
     * Instantiate a Metrics Configuration. Operation Modes are defined as below:
     * <pre>
     *     ONLINE: publish if connected to TAK ML Server
     *     DISABLED: don't record metrics
     *     OFFLINE: record metrics locally only
     *     OFFLINE_PUSH_ON_RECONNECT: record metrics locally and publish on connection to TAK ML Server
     * </pre>
     * @param operationMode - The operation mode
     */
    public MetricsConfiguration(Mode operationMode){
        this.operationMode = operationMode;
    }

    /**
     * Set publish rate in milliseconds. This will queue inference results metrics, queue them, and
     * publish them at the provided rate.
     *
     * <pre>
     * "0": constant
     * "1000": 1 second
     * etc.
     * </ul>
     *
     * @param publishRateSeconds - the period / rate in seconds for publishing of inference metrics
     */
    public void setPublishRate(long publishRateSeconds){
        this.publishRateSeconds = publishRateSeconds;
    }

    /**
     * Get the operation mode
     *
     * @return {@link Mode}
     */
    public Mode getOperationMode() {
        return operationMode;
    }

    public Long getPublishRateSeconds() {
        return publishRateSeconds;
    }
}
