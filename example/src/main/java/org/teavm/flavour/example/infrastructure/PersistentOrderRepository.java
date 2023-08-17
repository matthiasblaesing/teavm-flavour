/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.flavour.example.infrastructure;

import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.teavm.flavour.example.model.Order;
import org.teavm.flavour.example.model.OrderRepository;

@Component
public class PersistentOrderRepository extends PersistentRepositoryTemplate<Order> implements OrderRepository {
    @Autowired
    public PersistentOrderRepository(EntityManager em) {
        super(em, Order.class);
    }
}
