/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.extensions.amps;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.amps.config.Configuration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AMPSMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(AMPSMonitor.class);


    private static final String CONFIG_FILE = "config-file";
    private static final String CONFIG_FILE_PATH = "monitors/AMPSMonitor/config.yml";

    /**
     * Constructor that logs and prints the version number of this extension
     */
    public AMPSMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    /**
     * Hash map that contains the default settings of our config values
     */
    public static final Map<String, String> DEFAULT_ARGS = new HashMap<String, String>() {
        {
            put(CONFIG_FILE, CONFIG_FILE_PATH);
        }
    };

    /**
     * Main execution method that uploads the metrics to the AppDynamics
     * Controller
     *
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(Map,
     * com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        SimpleHttpClient httpClient = null;
        try {
            logger.info("Executing AMPSMonitor...");

            taskArguments = ArgumentsValidator.validateArguments(taskArguments, DEFAULT_ARGS);

            String configFilename = getConfigFilename(taskArguments.get(CONFIG_FILE));
            Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);

            Map<String, String> args = buildArgs(config);

            httpClient = SimpleHttpClient.builder(args).build();

            AMPSWrapper AMPSWrapper = new AMPSWrapper();

            //
            Map<String, Double> ampsMetrics = AMPSWrapper.gatherAMPSMetrics(httpClient);
            printMetricsHelper(config.getMetricPrefix(), config.getDisabledMetrics(), ampsMetrics);

            logger.info("Printed metrics successfully");
            return new TaskOutput("Task successfully...");
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return new TaskOutput("Task failed with errors");
    }

    private Map<String, String> buildArgs(Configuration config) {

        Map<String, String> args = new HashMap<String, String>();

        args.put(TaskInputArgs.HOST, config.getHost());
        args.put(TaskInputArgs.PORT, config.getPort() + "");
        args.put(TaskInputArgs.USE_SSL, Boolean.toString(config.isUseSSL()));
        args.put(TaskInputArgs.USER, config.getUsername());
        args.put(TaskInputArgs.PASSWORD, getPassword(config));
        return args;
    }

    private String getPassword(Configuration configuration) {
        String password = configuration.getPassword();

        String encryptedPassword = configuration.getPasswordEncrypted();

        if ((password == null || password.length() <= 0) && (encryptedPassword != null && encryptedPassword.length() > 0)) {
            try {
                Map<String, String> args = Maps.newHashMap();
                args.put(TaskInputArgs.PASSWORD_ENCRYPTED, encryptedPassword);
                args.put(TaskInputArgs.ENCRYPTION_KEY, configuration.getEncryptionKey());
                password = CryptoUtil.getPassword(args);

            } catch (IllegalArgumentException e) {
                String msg = "Encryption Key not specified. Please set the value in config.yaml.";
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        return password;
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     * @param aggregation Average OR Observation OR Sum
     * @param timeRollup  Average OR Current OR Sum
     * @param cluster     Collective OR Individual
     */
    private void printMetric(String metricName, Double metricValue, String aggregation, String timeRollup, String cluster) throws Exception {
        MetricWriter metricWriter = super.getMetricWriter(metricName, aggregation, timeRollup, cluster);
        if (metricValue != null) {
            metricWriter.printMetric(String.valueOf((long) metricValue.doubleValue()));
        }
    }

    /**
     * Print helper function. Concerned only with printing the metric map
     *
     * @param metricPrefix    Prefix identifying the metric to be a cluster, node, or bucket
     *                        metric
     * @param disabledMetrics
     * @param metricsMap
     */
    private void printMetricsHelper(String metricPrefix, Set<String> disabledMetrics, Map<String, Double> metricsMap) throws Exception {

        List<Pattern> disabledMetricsPatterns = buildPatterns(disabledMetrics);

        for (Entry<String, Double> entry : metricsMap.entrySet()) {
            String metricName = entry.getKey();
            Double metric = entry.getValue();
            if (!isMatched(disabledMetricsPatterns, metricName)) {
                printMetric(metricPrefix + metricName, metric, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
    }

    private List<Pattern> buildPatterns(Set<String> patterns) {
        if (patterns == null || patterns.size() <= 0) {
            return new ArrayList<Pattern>();
        }

        List<Pattern> excludePatterns = new ArrayList<Pattern>();
        for (String pattern : patterns) {
            try {
                Pattern compile = Pattern.compile(pattern);
                excludePatterns.add(compile);
            } catch (PatternSyntaxException ex) {
                logger.error("Invalid pattern[" + pattern + "] specified. Ignoring it.");
            }
        }

        return excludePatterns;
    }

    private boolean isMatched(List<Pattern> patterns, String input) {
        boolean isMatched = false;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).matches()) {
                logger.debug(" [" + input + "] matched");
                isMatched = true;
                break;
            }
        }
        return isMatched;
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    private static String getImplementationVersion() {
        return AMPSMonitor.class.getPackage().getImplementationTitle();
    }
}

