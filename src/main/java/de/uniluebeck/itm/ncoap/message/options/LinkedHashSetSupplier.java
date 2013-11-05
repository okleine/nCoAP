package de.uniluebeck.itm.ncoap.message.options;

import com.google.common.base.Supplier;

import java.util.LinkedHashSet;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 05.11.13
 * Time: 22:28
 * To change this template use File | Settings | File Templates.
 */
public class LinkedHashSetSupplier implements Supplier<LinkedHashSet<Option>> {

    public static LinkedHashSetSupplier instance = new LinkedHashSetSupplier();

    private LinkedHashSetSupplier(){};

    public static LinkedHashSetSupplier getInstance(){
        return instance;
    }

    @Override
    public LinkedHashSet<Option> get() {
        return new LinkedHashSet<>();
    }
}
