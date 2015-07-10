/*
 * Copyright (C) 2015 Siara Logics (cc)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Arundale R.
 *
 */
package cc.siara.csv_ml_demo;

import javax.swing.JApplet;

/**
 * Applet wrapper for the Swing demo application Use following tag in HTML file
 * to deploy it:
 * 
 * <applet archive="csv_ml_swing_demo-1.0.0.jar"
 * code="cc.siara.csv_ml_demo.MultiLevelCSVApplet.class" height="600"
 * width="800">Your browser does not support java applets</applet>
 * 
 * @author Arundale R.
 */
public class MultiLevelCSVApplet extends JApplet {

    private static final long serialVersionUID = -4489657640672417334L;

    public void init() {
        new MultiLevelCSVSwingDemo(this.getContentPane());
    }

}
