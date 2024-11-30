/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.flavour.rest.test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.flavour.rest.RESTClient;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class RestIT {

    private TestService service = RESTClient.factory(TestService.class).createResource("http://localhost:8080");

    @Test
    public void passesQueryParams() throws Exception {
        assertEquals(5, service.sum(2, 3));
    }

    @Test
    public void passesPathParamInsideUrl() throws Exception {
        assertEquals(0, service.sum(10, 7, 3));
    }

    @Test
    public void canReceiveComplexObject() throws Exception {
        ComplexDemo cd = service.getComplexDemo();
        assertEquals("Hello", cd.getStringDemo());
        assertEquals(42.23, cd.getDoubleDemo(), 0.01);
        assertNotNull(cd.getNestedSemiComplexDemo());
        assertEquals("World", cd.getNestedSemiComplexDemo().getStringDemo());
        assertEquals(3.14, cd.getNestedSemiComplexDemo().getDoubleDemo(), 0.01);
    }

}
