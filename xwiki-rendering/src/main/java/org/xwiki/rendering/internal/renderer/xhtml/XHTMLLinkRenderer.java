/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.renderer.xhtml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxLinkRenderer;
import org.xwiki.rendering.listener.Attachment;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Renders a XWiki Link into XHTML.
 * 
 * @version $Id: $
 * @since 1.5RC1
 */
public class XHTMLLinkRenderer
{
    private DocumentAccessBridge documentAccessBridge;

    private RenderingConfiguration configuration;

    private AttachmentParser attachmentParser;

    private XWikiSyntaxLinkRenderer xwikiSyntaxLinkRenderer;

    public XHTMLLinkRenderer(DocumentAccessBridge documentAccessBridge, RenderingConfiguration configuration,
        AttachmentParser attachmentParser)
    {
        this.documentAccessBridge = documentAccessBridge;
        this.configuration = configuration;
        this.attachmentParser = attachmentParser;
        this.xwikiSyntaxLinkRenderer = new XWikiSyntaxLinkRenderer();
    }

    public void beginRender(XHTMLWikiPrinter printer, Link link, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        try {
            beginRenderLinkInternal(printer, link, isFreeStandingURI, parameters);
        } catch (Exception e) {
            // TODO: Handle error
        }
    }

    public void endRender(XHTMLWikiPrinter printer, Link link, boolean isFreeStandingURI, boolean generateLinkContent)
    {
        try {
            endRenderLinkInternal(printer, link, isFreeStandingURI, generateLinkContent);
        } catch (Exception e) {
            // TODO: Handle error
        }
    }

    private void beginRenderLinkInternal(XHTMLWikiPrinter printer, Link link, boolean isFreeStandingURI,
        Map<String, String> parameters) throws Exception
    {
        // Add an XML comment as a placeholder so that the XHTML parser can find the document name.
        // Otherwise it would be too difficult to transform a URL into a document name especially since
        // a link can refer to an external URL.
        printer.printXMLComment("startwikilink:" + this.xwikiSyntaxLinkRenderer.renderLinkReference(link));

        Map<String, String> spanAttributes = new LinkedHashMap<String, String>();
        Map<String, String> aAttributes = new LinkedHashMap<String, String>();

        // Add all parameters to the A attributes
        aAttributes.putAll(parameters);

        if (link.isExternalLink()) {
            spanAttributes.put("class", "wikiexternallink");
            if (isFreeStandingURI) {
                aAttributes.put("class", "wikimodel-freestanding");
            }

            // href attribute
            if (link.getType() == LinkType.INTERWIKI) {
                // TODO: Resolve the Interwiki link
            } else {
                if (link.getReference().startsWith("attach:")) {
                    // use the default attachment syntax parser to extract document name and attchment name
                    Attachment attachment =
                        this.attachmentParser.parse(link.getReference().substring("attach:".length()));
                    aAttributes.put("href", this.documentAccessBridge.getAttachmentURL(attachment.getDocumentName(),
                        attachment.getAttachmentName()));
                } else {
                    aAttributes.put("href", link.getReference());
                }
            }

            printer.printXMLStartElement("span", spanAttributes);
            printer.printXMLStartElement("a", aAttributes);
        } else {
            // This is a link to a document.

            // Check for the document existence.
            if (link.getReference() == null || this.documentAccessBridge.exists(link.getReference())) {
                spanAttributes.put("class", "wikilink");
                aAttributes.put("href", this.documentAccessBridge.getURL(link.getReference(), "view", link
                    .getQueryString(), link.getAnchor()));
                printer.printXMLStartElement("span", spanAttributes);
                printer.printXMLStartElement("a", aAttributes);
            } else {
                spanAttributes.put("class", "wikicreatelink");
                aAttributes.put("href", this.documentAccessBridge.getURL(link.getReference(), "edit", link
                    .getQueryString(), link.getAnchor()));

                printer.printXMLStartElement("span", spanAttributes);
                printer.printXMLStartElement("a", aAttributes);
            }
        }
    }

    public void endRenderLinkInternal(XHTMLWikiPrinter printer, Link link, boolean isFreeStandingURI,
        boolean generateLinkContent) throws Exception
    {
        // If there was no link content then generate it based on the passed reference
        if (generateLinkContent) {
            printer.printXMLStartElement("span", new String[][] {{"class", "wikigeneratedlinkcontent"}});
            printer.printXML(link.getReference());
            printer.printXMLEndElement("span");
        }

        printer.printXMLEndElement("a");
        printer.printXMLEndElement("span");

        // Add a XML comment to signify the end of the link.
        printer.printXMLComment("stopwikilink");
    }
}
