/**
 * Copyright (c) 2011, Lukas Eder, lukas.eder@gmail.com
 * All rights reserved.
 *
 * This software is licensed to you under the Apache License, Version 2.0
 * (the "License"); You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * . Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * . Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * . Neither the name "JOOX" nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.joox.test;

import static java.util.Collections.nCopies;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtil;
import org.joox.Each;
import org.joox.Filter;
import org.joox.JOOX;
import org.joox.Mapper;
import org.joox.X;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * @author Lukas Eder
 */
public class JOOXTest {

    private String xmlString;
    private Document xmlDocument;
    private Element xmlElement;
    private int totalElements;
    private X $;
    private XPath xPath;

    @Before
    public void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        xmlString = IOUtil.toString(JOOXTest.class.getResourceAsStream("/example.xml"));
        xmlDocument = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));
        xmlElement = xmlDocument.getDocumentElement();
        $ = $(xmlDocument);
        xPath = XPathFactory.newInstance().newXPath();
        totalElements = ((Number) xPath
            .evaluate("count(//*)", xmlDocument, XPathConstants.NUMBER))
            .intValue() - 1;

    }

    // -------------------------------------------------------------------------
    // DOM access
    // -------------------------------------------------------------------------

    @Test
    public void testSize() {
        assertEquals(1, $.size());
        assertEquals(3, $.children().size());
    }

    // -------------------------------------------------------------------------
    // Traversing
    // -------------------------------------------------------------------------

    @Test
    public void testAdd() {
        assertEquals(0, $().size());
        assertEquals(1, $().add(xmlElement).size());
        assertEquals(1, $().add(xmlElement, xmlElement).size());

        X x = $().add(
            (Element) xmlElement.getElementsByTagName("director").item(0),
            (Element) xmlElement.getElementsByTagName("actor").item(0));
        assertEquals(2, x.size());
        assertEquals("director", x.get(0).getTagName());
        assertEquals("actor", x.get(1).getTagName());

        x = x.add($(xmlElement).find("dvds"));
        assertEquals(3, x.size());
        assertEquals("director", x.get(0).getTagName());
        assertEquals("actor", x.get(1).getTagName());
        assertEquals("dvds", x.get(2).getTagName());

        x = x.add(x.filter("dvds").find());
        assertEquals(9, x.size());
        assertEquals(
            Arrays.asList("dvds", "dvd", "name", "directors", "director", "actors", "actor", "actor", "actor"),
            x.tags());
    }

    @Test
    public void testChildren() {
        assertEquals("library", $.children().get().get(0).getTagName());
        assertEquals("library", $.children().get().get(1).getTagName());
        assertEquals("library", $.children().get().get(2).getTagName());
        assertEquals(2, $($.children().get().get(0)).children().size());
        assertEquals("books", $($.children().get().get(0)).children().get().get(0).getTagName());
        assertEquals(5, $($.children().get().get(0)).children().children().size());
        assertEquals("book", $($.children().get().get(0)).children().children().get().get(0).getTagName());
        assertEquals("1", $($.children().get().get(0)).children().children().get().get(0).getAttribute("id"));
        assertEquals("2", $($.children().get().get(0)).children().children().get().get(1).getAttribute("id"));
        assertEquals("3", $($.children().get().get(0)).children().children().get().get(2).getAttribute("id"));
        assertEquals("4", $($.children().get().get(0)).children().children().get().get(3).getAttribute("id"));

        assertEquals(4, $.children().children().size());
        assertEquals("books", $.children().children().get().get(0).getTagName());
        assertEquals("dvds", $.children().children().get().get(1).getTagName());
        assertEquals("books", $.children().children().get().get(2).getTagName());
        assertEquals("books", $.children().children().get().get(3).getTagName());
    }

    @Test
    public void testChildrenSelector() {
        assertEquals(0, $.children("document").size());
        assertEquals(0, $.children("asdf").size());
        assertEquals(3, $.children("library").size());
    }

    @Test
    public void testChildrenFilter() {
        assertEquals(0, $.children(JOOX.none()).size());
        assertEquals(1, $.children().children(JOOX.tag("dvds")).size());
        assertEquals(1, $.children().children(JOOX.tag("dvds")).children(JOOX.tag("dvd")).size());
    }

    @Test
    public void testEach() {
        final Queue<Integer> queue = new LinkedList<Integer>();

        queue.addAll(Arrays.asList(0));
        $.each(new Each() {
            @Override
            public void each(int index, Element element) {
                assertEquals((int) queue.poll(), index);
                assertEquals("document", element.getTagName());
            }
        });

        assertTrue(queue.isEmpty());
        queue.addAll(Arrays.asList(0, 1, 2));

        $.children().each(new Each() {
            @Override
            public void each(int index, Element element) {
                assertEquals((int) queue.poll(), index);
                assertEquals("library", element.getTagName());
            }
        });

        assertTrue(queue.isEmpty());
    }

    @Test
    public void testEq() {
        assertEquals("authors", $.find().eq(4).tag(0));
        assertEquals("author", $.find().eq(5).tag(0));
        assertEquals("George Orwell", $.find().eq(5).text());
    }

    @Test
    public void testFilter() {
        assertEquals(0, $.filter("asdf").size());
        assertEquals(1, $.filter("document").size());
        assertEquals(3, $.find().filter("actor").size());
        assertEquals(3, $.find().filter("actor").filter(JOOX.all()).size());
        assertEquals(2, $.find().filter("actor").filter(JOOX.even()).size());
        assertEquals(1, $.find().filter("actor").filter(JOOX.odd()).size());
    }

    @Test
    public void testFind() {
        assertEquals(0, $.find("document").size());
        assertEquals(0, $.find("asdf").size());
        assertEquals(0, $.find("document").find("document").size());
        assertEquals(0, $.find("document").find("libary").size());
        assertEquals(3, $.find("library").size());
        assertEquals(8, $.find("book").size());
        assertEquals(8, $.find("book").get().size());
        assertEquals("book", $.find("book").get().get(2).getTagName());
        assertEquals("book", $.find("book").get(2).getTagName());
        assertEquals("4", $.find("book").get().get(3).getAttribute("id"));
        assertEquals("4", $.find("book").get(3).getAttribute("id"));
    }

    @Test
    public void testFindFilter() throws Exception {
        assertEquals(0, $.find(JOOX.none()).size());
        assertEquals(totalElements, $.find().size());
        assertEquals(totalElements, $.find(JOOX.all()).size());
        assertEquals((totalElements + 1) / 2, $.find(JOOX.even()).size());
        assertEquals(totalElements / 2, $.find(JOOX.odd()).size());
        assertEquals(3, $.find(JOOX.tag("library")).size());
        assertEquals(8, $.find(JOOX.tag("book")).size());
    }

    @Test
    public void testFirst() throws Exception {
        assertEquals(0, $.find("document").first().size());
        assertEquals(1, $.first().size());
        assertEquals("document", $.first().tag());
        assertEquals("books", $.children().first().children().first().tag());
    }

    @Test
    public void testHas() throws Exception {
        assertEquals(0, $.has("asdf").size());
        assertEquals(0, $.has("document").size());
        assertEquals(1, $.has("library").size());
        assertEquals(1, $.has("authors").size());
        assertEquals(3, $.children().has("authors").size());
        assertEquals(1, $.children().has("dvds").size());
    }

    @Test
    public void testIs() throws Exception {
        assertFalse($.is("abc"));
        assertTrue($.is("document"));
        assertTrue($.is(JOOX.even()));
    }

    @Test
    public void testLast() throws Exception {
        assertEquals(0, $.find("document").last().size());
        assertEquals(1, $.last().size());
        assertEquals("document", $.last().tag());
        assertEquals("dvds", $.children().eq(0).children().last().tag());
    }

    @Test
    public void testMap() throws Exception {
        assertEquals(
            Arrays.asList("1", "2", "3", "4", "1", "3", "1", "2"),
            $.find("book").map(JOOX.ids()));

        assertEquals(
            Arrays.asList("Amazon", "Rösslitor", "Orell Füssli"),
            $.find("library").map(JOOX.attrs("name")));

        assertEquals(Arrays.asList(0, 1, 2, 3), $.children().first().find("book").map(new Mapper<Integer>() {
            @Override
            public Integer map(int index, Element element) {
                return index;
            }
        }));
    }

    @Test
    public void testNext() throws Exception {
        assertEquals(0, $.next().size());
        assertEquals(5, $.find("book").next().size());
        assertEquals(2, $.find("book").next().next().size());
        assertEquals(1, $.find("book").next().next().next().size());
        assertEquals(0, $.find("book").next().next().next().next().size());

        assertEquals(1, $.find("book").eq(0).next().size());
        assertEquals(1, $.find("book").eq(0).next(JOOX.all()).size());
        assertEquals(0, $.find("book").eq(0).next(JOOX.none()).size());
        assertEquals(0, $.find("book").eq(0).next(new Filter() {
            @Override
            public boolean filter(int index, Element element) {
                return "Paulo Coelho".equals($(element).find("author").text());
            }
        }).size());
        assertEquals(1, $.find("book").eq(1).next(new Filter() {
            @Override
            public boolean filter(int index, Element element) {
                return "Paulo Coelho".equals($(element).find("author").text());
            }
        }).size());
    }

    @Test
    public void testNextAll() throws Exception {
        assertEquals(0, $.nextAll().size());
        assertEquals(5, $.find("book").nextAll().size());
        assertEquals(2, $.find("book").nextAll().nextAll().size());
        assertEquals(1, $.find("book").nextAll().nextAll().nextAll().size());
        assertEquals(0, $.find("book").nextAll().nextAll().nextAll().nextAll().size());

        assertEquals(3, $.find("book").eq(0).nextAll().size());
        assertEquals(2, $.find("book").eq(0).nextAll().nextAll().size());
    }

    @Test
    public void testNextUntil() throws Exception {
        assertEquals(0, $.nextUntil("asdf").size());
        assertEquals(2, $.find("dvd").children().eq(0).nextUntil("any").size());
        assertEquals(
            Arrays.asList("directors", "actors"),
            $.find("dvd").children().eq(0).nextUntil("any").tags());
        assertEquals(1,
            $.find("dvd").children().eq(0).nextUntil("actors").size());
        assertEquals("directors",
            $.find("dvd").children().eq(0).nextUntil("actors").tag());
    }

    @Test
    public void testParent() throws Exception {
        assertEquals(0, $.parent().size());
        assertEquals(3, $.find("book").parent().size());
        assertEquals(nCopies(3, "books"), $.find("book").parent().tags());
        assertEquals(nCopies(8, "book"), $.find("authors").parent().tags());
    }

    @Test
    public void testParents() throws Exception {
        assertEquals(0, $.parents().size());
        assertEquals(1, $.find("library").parents().size());
        assertEquals(4, $.find("books").parents().size());
        assertEquals(7, $.find("book").parents().size());
        assertEquals(15, $.find("authors").parents().size());
        assertEquals(23, $.find("author").parents().size());
        assertEquals(26, $.find("author").add($.find("actor")).parents().size());
    }

    @Test
    public void testParentsUntil() throws Exception {
        assertEquals(0, $.parentsUntil("books").size());
        assertEquals(1, $.find("library").parentsUntil("books").size());
        assertEquals(4, $.find("books").parentsUntil("books").size());
        assertEquals(0, $.find("book").parentsUntil("books").size());
        assertEquals(8, $.find("authors").parentsUntil("books").size());
        assertEquals(16, $.find("author").parentsUntil("books").size());
        assertEquals(21, $.find("author").add($.find("actor")).parentsUntil("books").size());
    }

    @Test
    public void testPrev() throws Exception {
        assertEquals(0, $.prev().size());
        assertEquals(5, $.find("book").prev().size());
        assertEquals(2, $.find("book").prev().prev().size());
        assertEquals(1, $.find("book").prev().prev().prev().size());
        assertEquals(0, $.find("book").prev().prev().prev().prev().size());

        assertEquals(1, $.find("book").eq(7).prev().size());
        assertEquals(1, $.find("book").eq(7).prev(JOOX.all()).size());
        assertEquals(0, $.find("book").eq(7).prev(JOOX.none()).size());
        assertEquals(0, $.find("book").eq(7).prev(new Filter() {
            @Override
            public boolean filter(int index, Element element) {
                return "Paulo Coelho".equals($(element).find("author").text());
            }
        }).size());
        assertEquals(1, $.find("book").eq(3).prev(new Filter() {
            @Override
            public boolean filter(int index, Element element) {
                return "Paulo Coelho".equals($(element).find("author").text());
            }
        }).size());
    }

    @Test
    public void testPrevAll() throws Exception {
        assertEquals(0, $.prevAll().size());
        assertEquals(5, $.find("book").prevAll().size());
        assertEquals(2, $.find("book").prevAll().prevAll().size());
        assertEquals(1, $.find("book").prevAll().prevAll().prevAll().size());
        assertEquals(0, $.find("book").prevAll().prevAll().prevAll().prevAll().size());

        assertEquals(3, $.find("book").eq(3).prevAll().size());
        assertEquals(2, $.find("book").eq(3).prevAll().prevAll().size());
    }

    @Test
    public void testPrevUntil() throws Exception {
        assertEquals(0, $.prevUntil("asdf").size());
        assertEquals(2, $.find("dvd").children().eq(2).prevUntil("any").size());
        assertEquals(
            Arrays.asList("name", "directors"),
            $.find("dvd").children().eq(2).prevUntil("any").tags());
        assertEquals(1,
            $.find("dvd").children().eq(2).prevUntil("name").size());
        assertEquals("directors",
            $.find("dvd").children().eq(2).prevUntil("name").tag());
    }

    @Test
    public void testSiblings() throws Exception {
        assertEquals(0, $.siblings().size());
        assertEquals(3, $.find("library").siblings().size());
        assertEquals(
            Arrays.asList("library", "library", "library"),
            $.find("library").siblings().tags());
        assertEquals(2, $.find("library").eq(0).siblings().size());
        assertEquals(2, $.find("library").eq(1).siblings().size());
        assertEquals(2, $.find("library").eq(2).siblings().size());
        assertEquals(
            Arrays.asList("library", "library"),
            $.find("library").eq(0).siblings().tags());
        assertEquals(0, $.find("library").eq(3).siblings().size());
    }

    @Test
    public void testSlice() throws Exception {
        assertEquals(0, $.slice(1).size());
        assertEquals(1, $.slice(0).size());
        assertEquals(1, $.slice(-1).size());
        assertEquals(1, $.slice(-2).size());

        assertEquals(0, $.slice(1, 1).size());
        assertEquals(1, $.slice(0, 1).size());
        assertEquals(1, $.slice(-1, 1).size());
        assertEquals(1, $.slice(-2, 1).size());

        assertEquals(8, $.find("book").slice(-9).size());
        assertEquals(8, $.find("book").slice(-8).size());
        assertEquals(2, $.find("book").slice(-2).size());
        assertEquals(1, $.find("book").slice(-1).size());
        assertEquals(8, $.find("book").slice(0).size());
        assertEquals(7, $.find("book").slice(1).size());
        assertEquals(6, $.find("book").slice(2).size());
        assertEquals(5, $.find("book").slice(3).size());
        assertEquals(4, $.find("book").slice(4).size());
        assertEquals(3, $.find("book").slice(5).size());
        assertEquals(2, $.find("book").slice(6).size());
        assertEquals(1, $.find("book").slice(7).size());
        assertEquals(0, $.find("book").slice(8).size());
        assertEquals(0, $.find("book").slice(9).size());

        assertEquals(5, $.find("book").slice(-9, 5).size());
        assertEquals(5, $.find("book").slice(-8, 5).size());
        assertEquals(0, $.find("book").slice(-2, 5).size());
        assertEquals(0, $.find("book").slice(-1, 5).size());
        assertEquals(5, $.find("book").slice(0, 5).size());
        assertEquals(4, $.find("book").slice(1, 5).size());
        assertEquals(3, $.find("book").slice(2, 5).size());
        assertEquals(2, $.find("book").slice(3, 5).size());
        assertEquals(1, $.find("book").slice(4, 5).size());
        assertEquals(0, $.find("book").slice(5, 5).size());
        assertEquals(0, $.find("book").slice(6, 5).size());
        assertEquals(0, $.find("book").slice(7, 5).size());
        assertEquals(0, $.find("book").slice(8, 5).size());
        assertEquals(0, $.find("book").slice(9, 5).size());

        assertEquals(3, $.find("book").slice(-9, -5).size());
        assertEquals(3, $.find("book").slice(-8, -5).size());
        assertEquals(0, $.find("book").slice(-2, -5).size());
        assertEquals(0, $.find("book").slice(-1, -5).size());
        assertEquals(3, $.find("book").slice(0, -5).size());
        assertEquals(2, $.find("book").slice(1, -5).size());
        assertEquals(1, $.find("book").slice(2, -5).size());
        assertEquals(0, $.find("book").slice(3, -5).size());
        assertEquals(0, $.find("book").slice(4, -5).size());
        assertEquals(0, $.find("book").slice(5, -5).size());
        assertEquals(0, $.find("book").slice(6, -5).size());
        assertEquals(0, $.find("book").slice(7, -5).size());
        assertEquals(0, $.find("book").slice(8, -5).size());
        assertEquals(0, $.find("book").slice(9, -5).size());
    }

    @Test
    public void testDOMAccess() throws Exception {
        assertEquals(xmlElement, $.get(0));
        assertEquals(xmlElement, $.get().get(0));
        assertNull($.get(1));

        assertEquals("document", $.tag());
        assertEquals("document", $.tags().get(0));
        assertEquals("document", $.tag(0));
        assertNull($.tag(1));
        assertNull($.next().tag());
        assertNull($.next().tag(0));
        assertNull($.next().tag(1));
    }

    @Test
    public void testAndOrNot() throws Exception {
        assertEquals(1, $.filter(JOOX.and(JOOX.all(), JOOX.all())).size());
        assertEquals(0, $.filter(JOOX.and(JOOX.all(), JOOX.none())).size());
        assertEquals(0, $.filter(JOOX.and(JOOX.none(), JOOX.all())).size());
        assertEquals(0, $.filter(JOOX.and(JOOX.none(), JOOX.none())).size());

        assertEquals(1, $.filter(JOOX.or(JOOX.all(), JOOX.all())).size());
        assertEquals(1, $.filter(JOOX.or(JOOX.all(), JOOX.none())).size());
        assertEquals(1, $.filter(JOOX.or(JOOX.none(), JOOX.all())).size());
        assertEquals(0, $.filter(JOOX.or(JOOX.none(), JOOX.none())).size());

        assertEquals(0, $.filter(JOOX.not(JOOX.all())).size());
        assertEquals(1, $.filter(JOOX.not(JOOX.none())).size());
    }

    @Test
    public void testAttr() throws Exception {
        assertNull($.attr("any"));
        assertNull($.attr("id"));
        assertEquals(Arrays.asList((String) null), $.attrs("any"));
        assertEquals(Arrays.asList((String) null), $.attrs("id"));
        assertEquals("1", $.find("book").attr("id"));
        assertEquals(
            Arrays.asList("1", "2", "3", "4", "1", "3", "1", "2"),
            $.find("book").attrs("id"));

        assertEquals(
            Collections.nCopies(totalElements, "y"),
            $.find().attr("x", "y").attrs("x"));
        assertEquals(
            Collections.nCopies(totalElements, (String) null),
            $.find().attr("x", (String) null).attrs("x"));

        assertEquals(
            Collections.nCopies(totalElements, (String) null),
            $.find().removeAttr("id").attrs("id"));
    }

    @Test
    public void testEmpty() throws Exception {
        assertEquals(0, $.find("directors").empty().find().size());
        assertEquals(0, $.find("director").size());
        assertEquals(1, $.empty().size());
        assertEquals(0, $.find().size());
    }

    @Test
    public void testRemove() throws Exception {
        assertEquals(0, $.find("director").remove().size());
        assertEquals(0, $.find("director").size());
        assertEquals(3, $.find("book").remove(JOOX.ids("1", "2")).size());
        assertEquals(3, $.find("book").remove(JOOX.ids("1", "2")).size());
        assertEquals(0, $.remove().size());
        assertEquals(0, $.find().size());
        assertEquals(0, $.size());
    }

    @Test
    public void testText() throws Exception {
        assertNull($.find("any").text());
        assertEquals("Sergio Leone", $.find("director").text());
        assertEquals("Charles Bronson", $.find("actor").text());
        assertEquals(
            Arrays.asList("Charles Bronson", "Jason Robards", "Claudia Cardinale"),
            $.find("actor").texts());

        assertEquals(
            Collections.nCopies(3, "Lukas Eder"),
            $.find("actor").text("Lukas Eder").texts());

        assertEquals("<abc/>", $.find("actors").text("<abc/>").text());
        assertEquals("<><aa>", $.find("actors").text("<><aa>").text());
    }

    @Test
    public void testContent() throws Exception {
        assertEquals("Sergio Leone", $.find("director").content());
        assertEquals(Arrays.asList(
            "Charles Bronson",
            "Jason Robards",
            "Claudia Cardinale"),
            $.find("actor").contents());

        assertEquals("<><aa>", $.find("actors").content("<><aa>").text());
        assertEquals("<><aa>", $.find("actors").content());
        assertEquals("<abc><x></abc>", $.find("actors").content("<abc><x></abc>").text());
        assertEquals("<abc><x></abc>", $.find("actors").content());
        assertEquals("", $.find("actors").content("<abc><x/></abc>").text());
        assertEquals("<abc><x/></abc>", $.find("actors").content());
        assertEquals(1, $.find("abc").size());
        assertEquals(1, $.find("x").size());
        assertEquals(8, $.find("book").content("<book-content/>").size());
        assertEquals(8, $.find("book-content").size());
        assertEquals(
            Collections.nCopies(8, "book"),
            $.find("book-content").parent().tags());

        assertEquals("<xx/><xx/>", $.find("actors").content("<xx/><xx/>").content());
        assertEquals(2, $.find("xx").size());
    }

    @Test
    public void testAfter() throws Exception {
        assertEquals(2, $.find("dvds").after("<cds/>").size());
        assertEquals(1, $.find("cds").size());
        assertEquals(3, $.find("library").eq(0).children().size());
        assertEquals(
            Arrays.asList("books", "dvds", "cds"),
            $.find("library").eq(0).children().tags());

        assertEquals(2, $.find("dvds").after("<postcards/>").size());
        assertEquals(1, $.find("postcards").size());
        assertEquals(4, $.find("library").eq(0).children().size());
        assertEquals(
            Arrays.asList("books", "dvds", "postcards", "cds"),
            $.find("library").eq(0).children().tags());
    }

    @Test
    public void testBefore() throws Exception {
        assertEquals(2, $.find("dvds").before("<cds/>").size());
        assertEquals(1, $.find("cds").size());
        assertEquals(3, $.find("library").eq(0).children().size());
        assertEquals(
            Arrays.asList("books", "cds", "dvds"),
            $.find("library").eq(0).children().tags());

        assertEquals(2, $.find("dvds").before("<postcards/>").size());
        assertEquals(1, $.find("postcards").size());
        assertEquals(4, $.find("library").eq(0).children().size());
        assertEquals(
            Arrays.asList("books", "cds", "postcards", "dvds"),
            $.find("library").eq(0).children().tags());
    }

    @Test
    public void testAppend() throws Exception {
        assertEquals(1, $.find("dvds").append("<dvd id=\"6\"/>").size());
        assertEquals(2, $.find("dvd").size());
        assertEquals(2, $.find("dvds").children().size());
        assertEquals(
            Arrays.asList("5", "6"),
            $.find("dvd").ids());

        assertEquals(1, $.find("dvds").append("<dvd id=\"7\"/><dvd id=\"8\"/>").size());
        assertEquals(4, $.find("dvd").size());
        assertEquals(4, $.find("dvds").children().size());
        assertEquals(
            Arrays.asList("5", "6", "7", "8"),
            $.find("dvd").ids());

        assertEquals(1, $.find("director").append("<><aa>").size());
        assertEquals(0, $.find("director").children().size());
        assertEquals("Sergio Leone<><aa>", $.find("director").text());
        assertEquals("Sergio Leone<><aa>", $.find("director").content());
    }

    @Test
    public void testPrepend() throws Exception {
        assertEquals(1, $.find("dvds").prepend("<dvd id=\"6\"/>").size());
        assertEquals(2, $.find("dvd").size());
        assertEquals(2, $.find("dvds").children().size());
        assertEquals(
            Arrays.asList("6", "5"),
            $.find("dvd").ids());

        assertEquals(1, $.find("dvds").prepend("<dvd id=\"7\"/><dvd id=\"8\"/>").size());
        assertEquals(4, $.find("dvd").size());
        assertEquals(4, $.find("dvds").children().size());
        assertEquals(
            Arrays.asList("7", "8", "6", "5"),
            $.find("dvd").ids());

        assertEquals(1, $.find("director").prepend("<><aa>").size());
        assertEquals(0, $.find("director").children().size());
        assertEquals("<><aa>Sergio Leone", $.find("director").text());
        assertEquals("<><aa>Sergio Leone", $.find("director").content());
    }

    @Test
    public void testReplaceWith() throws Exception {
        assertEquals(
            "best-director-in-the-world",
            $.find("director").replaceWith("<best-director-in-the-world>Jean Claude van Damme</best-director-in-the-world>").tag());
        assertEquals(0, $.find("director").size());
        assertEquals(1, $.find("best-director-in-the-world").size());
        assertEquals("directors", $.find("best-director-in-the-world").parent().tag());

        assertEquals(0, $.find("best-director-in-the-world").replaceWith("<><aa>").size());
        assertEquals("<><aa>", $.find("directors").text().trim());
        assertEquals("<><aa>", $.find("directors").content().trim());
    }

    @Test
    public void testEmptyJOOX() throws Exception {
        X x = JOOX.$();
        assertEquals(0, x.size());
        assertEquals(0, x.children().size());
        assertEquals(0, x.find("any").size());
        assertEquals(0, x.append("<hello><world/></hello>").size());
    }
}
