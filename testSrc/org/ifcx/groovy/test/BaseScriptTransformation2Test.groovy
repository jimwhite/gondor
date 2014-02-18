/*
* Copyright 2008-2013 the original author or authors.
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

package org.ifcx.groovy.test

import groovy.transform.BaseScript

abstract class DeclaredBaseScript2 extends Script {
//    int meaningOfLife = 1
    int _meaningOfLife = 1
    def getProperty(String name) { "meaningOfLife".equals(name) ? _meaningOfLife : super.getProperty(name); }
    void setProperty(String name, Object v) { "meaningOfLife".equals(name) ? _meaningOfLife = v : super.setProperty(name, v); }
}

@BaseScript DeclaredBaseScript2 baseScript


assert binding.variables.size() == 1
assert meaningOfLife == 1
assert binding.variables.size() == 1
meaningOfLife = 2
assert binding.variables.size() == 1
assert meaningOfLife == 2
