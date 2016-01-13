/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.core.firstrun;

import de.ii.xsf.core.api.firstrun.FirstRunPage;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class FirstRunDonePage implements FirstRunPage {
    
    @Override
    public String getTitle() {
        return "Configuration done.";
    }

    @Override
    public String getDescription() {
        return "Click on <b>next</b> to login.";
    }

    @Override
    public String getForm() {
        return null;
    }
    
    @Override
    public boolean needsConfig() {
        return false;
    }
    
    @Override
    public void setResult( Map<String,String[]> result){
        // nothing to do
    }

    @Override
    public boolean isFirstPage() {
        return false;
    }
    
    @Override
    public boolean configIsDone() {
        return false;
    }
}
