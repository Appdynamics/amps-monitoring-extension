/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.amps;


import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AMPSMonitorTest {

    @Test
    public void runSuccessfullyWhenGivenEncryptedPassword() throws TaskExecutionException {
        Map<String, String> taskArgs = getMap();
        AMPSMonitor monitor = new AMPSMonitor();
        monitor.execute(taskArgs,new TaskExecutionContext());
    }

    private Map<String, String> getMap() {
        Map<String,String> taskArgs = new HashMap<String,String>();

        taskArgs.put("config-file","src/test/resources/conf/config.yml");
        return taskArgs;
    }
}

