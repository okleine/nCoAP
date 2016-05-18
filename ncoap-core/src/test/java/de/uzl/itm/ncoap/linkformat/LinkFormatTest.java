/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uzl.itm.ncoap.linkformat;

import de.uzl.itm.ncoap.AbstractCoapTest;
import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.application.linkformat.LinkValue;
import de.uzl.itm.ncoap.application.linkformat.LinkValueList;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;


/**
 * Created by olli on 29.04.16.
 */
public class LinkFormatTest extends AbstractCoapTest {

    private static String expected =
            "</obs>;obs;rt=\"observe\";title=\"Observable resource which changes every 5 seconds\"," +
            "</obs-pumping>;obs;rt=\"observe\";title=\"Observable resource which changes every 5 seconds\"," +
            "</separate>;title=\"Resource which cannot be served immediately and which cannot be acknowledged in a piggy-backed way\"," +
            "</large-create>;rt=\"block\";title=\"Large resource that can be created using POST method\"," +
            "</seg1>;title=\"Long path resource\"," +
            "</seg1/seg2>;title=\"Long path resource\"," +
            "</seg1/seg2/seg3>;title=\"Long path resource\"," +
            "</large-separate>;rt=\"block\";sz=1280;title=\"Large resource\"," +
            "</obs-reset>," +
            "</.well-known/core>," +
            "</multi-format>;ct=\"0 41\";title=\"Resource that exists in different content formats (text/plain utf8 and application/xml)\"," +
            "</path>;ct=40;title=\"Hierarchical link description entry\"," +
            "</path/sub1>;title=\"Hierarchical link description sub-resource\"," +
            "</path/sub2>;title=\"Hierarchical link description sub-resource\"," +
            "</path/sub3>;title=\"Hierarchical link description sub-resource\"," +
            "</link1>;if=\"If1\";rt=\"Type1 Type2\";title=\"Link test resource\"," +
            "</link3>;if=\"foo\";rt=\"Type1 Type3\";title=\"Link test resource\"," +
            "</link2>;if=\"If2\";rt=\"Type2 Type3\";title=\"Link test resource\"," +
            "</obs-large>;obs;rt=\"observe\";title=\"Observable resource which changes every 5 seconds\"," +
            "</validate>;ct=0;sz=17;title=\"Resource which varies\"," +
            "</test>;title=\"Default test resource\"," +
            "</large>;rt=\"block\";sz=1280;title=\"Large resource\"," +
            "</obs-pumping-non>;obs;rt=\"observe\";title=\"Observable resource which changes every 5 seconds\"," +
            "</query>;title=\"Resource accepting query parameters\"," +
            "</large-post>;rt=\"block\";title=\"Handle POST with two-way blockwise transfer\"," +
            "</location-query>;title=\"Perform POST transaction with responses containing several Location-Query options (CON mode)\"," +
            "</obs-non>;obs;rt=\"observe\";title=\"Observable resource which changes every 5 seconds\"," +
            "</large-update>;rt=\"block\";sz=1280;title=\"Large resource that can be updated using PUT method\"," +
            "</shutdown>";

    private static LinkValueList linkValueList;

    @Before
    public void decode() {
        linkValueList = LinkValueList.decode(expected);
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(LinkValueList.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(LinkValue.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(LinkParam.class.getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void testFilterByUriReference() {
        List<String> result = linkValueList.filter("/obs").getUriReferences();
        assertEquals("Wrong number of URI references found", 1, result.size());
    }

    @Test
    public void testFilterByUriPrefix() {
        List<String> result = linkValueList.filter("/obs*").getUriReferences();
        assertEquals("Wrong number of URI references found", 6, result.size());
    }

    @Test
    public void testLinkValueListContains29URIs() {
        List<String> result = linkValueList.getUriReferences();
        assertEquals("Wrong number of URI references found", 29, result.size());
    }

    @Test
    public void testTitleParam() {
        String title = "Observable resource which changes every 5 seconds";
        Set<String> result = linkValueList.getUriReferences(LinkParam.Key.TITLE, title);
        assertEquals("Wrong number of URI references found", 5, result.size());
    }

    @Test
    public void testContentType0() {
        Set<String> result = linkValueList.getUriReferences(LinkParam.Key.CT, "0");
        assertEquals("Wrong number of URI references found", 2, result.size());
    }

    @Test
    public void testContentType40() {
        Set<String> result = linkValueList.getUriReferences(LinkParam.Key.CT, "40");
        assertEquals("Wrong number of URI references found", 1, result.size());
    }

    @Test
    public void testContentType41() {
        Set<String> result = linkValueList.getUriReferences(LinkParam.Key.CT, "41");
        assertEquals("Wrong number of URI references found", 1, result.size());
    }

    @Test
    public void testContentType0_41() {
        Set<String> result = linkValueList.getUriReferences(LinkParam.Key.CT, "0 41");
        assertEquals("Wrong number of URI references found", 0, result.size());
    }

    @Test
    public void testFilterByEstimatedSize() {
        Set<String> result = linkValueList.getUriReferences(LinkParam.Key.SZ, "1280");
        assertEquals("Wrong number of URI references found", 3, result.size());
    }

}
