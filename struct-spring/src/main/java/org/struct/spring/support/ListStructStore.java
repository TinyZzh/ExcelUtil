/*
 *
 *
 *          Copyright (c) 2020. - TinyZ.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.struct.spring.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.struct.util.WorkerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Not support all <code>key</code> related method. like {@link #get(Object)}.
 *
 * @author TinyZ.
 * @version 2020.07.12
 */
public class ListStructStore<B> extends AbstractStructStore<Object, B> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListStructStore.class);

    /**
     * the cached struct data map.
     */
    private List<B> cached = new LinkedList<>();

    public ListStructStore() {
        //  spring bean definition
    }

    public ListStructStore(Class<B> clzOfBean) {
        super(clzOfBean);
    }

    @Override
    public void initialize() {
        if (!casStatusInit()) {
            return;
        }
        try {
            List<B> collected = WorkerUtil.newWorker(this.config.getWorkspace(), this.clzOfBean())
                    .toList(ArrayList::new);
            this.cached.clear();
            this.cached.addAll(collected);
            LOGGER.info("initialize struct successfully. identify:{}, clz:{}", this.identify(), this.clzOfBean);
        } catch (Exception e) {
            LOGGER.error("initialize struct failure. identify:{}, clz:{}", this.identify(), this.clzOfBean, e);
        } finally {
            casStatusDone();
        }
    }

    @Override
    public void dispose() {
        //  reset status.
        this.casStatusReset();
        this.cached.clear();
    }

    @Override
    public List<B> getAll() {
        return Collections.unmodifiableList(this.cached);
    }

    @Override
    public B get(Object key) {
        //  not implement
        throw new UnsupportedOperationException(this.getClass() + " Unsupported the @get operation.");
    }

    @Override
    public List<B> lookup(Predicate<B> filter) {
        return this.cached.stream().filter(filter).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "ListStructStore{" +
                "clzOfBean=" + clzOfBean +
                "} " + super.toString();
    }
}