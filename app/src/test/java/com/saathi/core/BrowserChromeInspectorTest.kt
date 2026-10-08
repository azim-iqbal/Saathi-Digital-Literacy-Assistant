package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class BrowserChromeInspectorTest {
    private class N(override val owner:String="com.android.chrome", override val className:String="android.view.View",
        override val resourceId:String?=null, override val visible:Boolean=true, override val enabled:Boolean=true,
        override val focused:Boolean=false, override val password:Boolean=false, override val editable:Boolean=false,
        val text:String?=null, val children:List<N?> = emptyList()): BrowserChromeInspector.Node {
        var reads=0; var released=false
        override val childCount get()=children.size
        override fun addressText():String? { reads++; return text }
        override fun child(index:Int)=children[index]
        override fun release() { released=true }
    }
    private fun bar(url:String="https://service.example/requirements", focused:Boolean=false, editable:Boolean=false)=
        N(resourceId="com.android.chrome:id/url_bar",text=url,focused=focused,editable=editable)
    @Test fun onlySupportedVisibleChromeAddressIsRead() {
        val b=bar(); val web=N(className="android.webkit.WebView",children=listOf(bar("https://evil.example/")))
        val root=N(children=listOf(b,web))
        assertEquals("https://service.example/requirements",BrowserChromeInspector.read(root))
        assertEquals(0,web.reads); assertTrue(b.released); assertFalse(root.released)
        assertNull(BrowserChromeInspector.read(N(owner="fake.browser",children=listOf(bar()))))
    }
    @Test fun spoofedBarInsideWebViewAndAmbiguousMissingBranchesAreRejected() {
        assertNull(BrowserChromeInspector.read(N(children=listOf(N(className="android.webkit.WebView",children=listOf(bar()))))))
        assertNull(BrowserChromeInspector.read(N(children=listOf(bar(),bar()))))
        assertNull(BrowserChromeInspector.read(N(children=listOf(bar(),null))))
        assertNull(BrowserChromeInspector.read(N(children=listOf(bar(),N(owner="unknown.app")))))
    }
    @Test fun partialDepthAndNodeScansDoNotEstablishAUniqueAddress() {
        var nested=N(); repeat(21) { nested=N(children=listOf(nested)) }
        assertNull(BrowserChromeInspector.read(N(children=listOf(bar(),nested))))
        assertNull(BrowserChromeInspector.read(N(children=listOf(bar())+List(150){N()})))
    }
    @Test fun editedAddressCannotBeMistakenForLoadedDestination() {
        for (b in listOf(bar(focused=true),bar(editable=true))) {
            assertNull(BrowserChromeInspector.read(N(children=listOf(b)))); assertEquals(0,b.reads)
        }
        for (url in listOf("service.example/requirements","https://service.example/requirements?private=value","https://127.0.0.1/"))
            assertNull(BrowserChromeInspector.read(N(children=listOf(bar(url)))))
    }
    @Test fun braveMappingDoesNotAcceptChromeOrWebPageIdentifiers() {
        val b=N(owner="com.brave.browser",resourceId="com.brave.browser:id/url_bar",text="https://service.example/")
        assertEquals("https://service.example/",BrowserChromeInspector.read(N(owner="com.brave.browser",children=listOf(b))))
        assertNull(BrowserChromeInspector.read(N(owner="com.brave.browser",children=listOf(bar()))))
    }
}
