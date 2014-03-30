package de.uniluebeck.itm.ncoap.application.server.webservice.linkformat;

/**
 * Created by olli on 30.03.14.
 */
public class EmptyLinkAttribute extends LinkAttribute<Void> {

    public EmptyLinkAttribute(String attributeKey, Void value) throws IllegalArgumentException {
        super(attributeKey, value);
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EmptyLinkAttribute))
            return false;

        EmptyLinkAttribute other = (EmptyLinkAttribute) object;

        return this.getKey().equals(other.getKey());
    }
}
