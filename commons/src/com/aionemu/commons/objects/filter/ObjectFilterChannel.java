package com.aionemu.commons.objects.filter;

/**
 *
 * 对象过滤器链：它包含所有的对象过滤器，对象只有通过所有的过滤器
 *
 */
public class ObjectFilterChannel<T> implements ObjectFilter<T> {

    private ObjectFilter<? super T>[] filters;

    public ObjectFilterChannel(ObjectFilter<? super T>... filters) {
        this.filters = filters;
    }

    /**
     * 接受对象
     * 
     * @param object
     * @return 
     */
    @Override
    public boolean acceptObject(T object) {
        for (ObjectFilter<? super T> filter : filters) {
            if (filter != null && !filter.acceptObject(object)) {
                return false;
            }
        }
        return true;
    }
}