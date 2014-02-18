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

abstract class DeclaredBaseScript3 extends Script {
    def _v = { it * 2 }
    def getProperty(String name) { "v".equals(name) ? _v : super.getProperty(name); }
    void setProperty(String name, Object v) { "v".equals(name) ? _v = v : super.setProperty(name, v); }
    def c = { it * 4 }
}

@BaseScript DeclaredBaseScript3 baseScript


assert c(2) == 8
assert v(2) == 4
v = { it * 3 }
assert v(2) == 6

