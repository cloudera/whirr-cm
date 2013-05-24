/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.whirr.cm;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest implements BaseTest {
    
    @Test
    public void testUrlForURI() throws Exception {
        Assert.assertNull(Utils.urlForURI("classpath:///file-that-does-not-exist-we-hope"));
                                
        Assert.assertNull(Utils.urlForURI("http://www.exmaple.com/non-existant-url"));

        Assert.assertEquals(Utils.urlForURI("classpath:///whirr-cm-default.properties"),
                            UtilsTest.class.getClassLoader().getResource("whirr-cm-default.properties"));

        File pom = new File("pom.xml");
        Assert.assertEquals(Utils.urlForURI("file://" + pom.getAbsolutePath()),
                            pom.toURI().toURL());
        
    }    
}