/**
 * Copyright (c) 2011-2012 Felix Mannhardt, felix.mannhardt@smail.wir.h-brs.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See: http://www.gnu.org/licenses/lgpl-3.0
 *
 */
package de.hbrs.oryx.yawl.converter.handler.oryx;

import de.hbrs.oryx.yawl.converter.context.OryxConversionContext;
import de.hbrs.oryx.yawl.converter.exceptions.NoRootNetFoundException;
import de.hbrs.oryx.yawl.converter.handler.HandlerFactory;
import de.hbrs.oryx.yawl.util.YAWLUtils;
import org.jdom.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oryxeditor.server.diagram.basic.BasicDiagram;
import org.oryxeditor.server.diagram.basic.BasicShape;
import org.yawlfoundation.yawl.elements.YSpecVersion;
import org.yawlfoundation.yawl.elements.YSpecification;
import org.yawlfoundation.yawl.exceptions.YSyntaxException;
import org.yawlfoundation.yawl.unmarshal.YMetaData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converts the Diagram to a YAWL specification
 *
 * @author Felix Mannhardt (Bonn-Rhein-Sieg University of Applied Sciences)
 */
public class OryxDiagramHandler extends OryxHandlerImpl {

    private final BasicDiagram diagramShape;

    public OryxDiagramHandler(OryxConversionContext context, BasicDiagram diagramShape) {
        super(context);
        this.diagramShape = diagramShape;
    }

    /*
      * (non-Javadoc)
      *
      * @see de.hbrs.oryx.yawl.converter.handler.oryx.OryxHandler#convert()
      */
    @Override
    public void convert() {

        try {
            BasicShape rootNet = findRootNet();

            // First extract specification details
            YSpecification yawlSpec = convertSpecification();
            getContext().setSpecification(yawlSpec);

            // Then go on converting the root net
            HandlerFactory handlerFactory = getContext().getHandlerFactory();
            handlerFactory.createOryxConverter(rootNet).convert();

            // Then convert layout using the stored information about net
            // layouts
            convertLayout();

        } catch (NoRootNetFoundException e) {
            getContext().addConversionWarnings(e);
            // Abort conversion
            getContext().setConversionError(true);
        }

    }

    private void convertLayout() {
        Element specLayout = new Element("layout", YAWLUtils.YAWL_NS);
        Element locale = new Element("locale", YAWLUtils.YAWL_NS);
        locale.setAttribute("language", "en");
        locale.setAttribute("country", "AU");
        Element spec = new Element("specification");
        spec.setAttribute("id", diagramShape.getProperty("specuri"));
        Element size = new Element("size");
        specLayout.addContent(locale);
        specLayout.addContent(spec);
        specLayout.addContent(size);
        List<Element> netLayoutList = getContext().getNetLayoutList();
        specLayout.addContent(netLayoutList);
        Element labelFontSize = new Element("labelFontSize");
        labelFontSize.setText("12");
        specLayout.addContent(labelFontSize);
        getContext().setSpecificationLayout(specLayout);
    }

    private BasicShape findRootNet() throws NoRootNetFoundException {
        if (isRootNet(diagramShape)) {
            return diagramShape;
        }
        // No root net found
        throw new NoRootNetFoundException("Could not find root net in this diagram."
                + "Please select a diagram containing a root net.");
    }

    private boolean isRootNet(BasicShape shape) {
        if (shape.hasProperty("isrootnet")) {
            return new Boolean(shape.getProperty("isrootnet"));
        } else {
            return false;
        }
    }

    private YSpecification convertSpecification() {
        YSpecification spec = new YSpecification(diagramShape.getProperty("specuri"));

        spec.setName(diagramShape.getProperty("specname"));

        try {
            spec.setSchema(diagramShape.getProperty("specdatatypedefinitions"));
        } catch (YSyntaxException e) {
            getContext().addConversionWarnings("Invalid Data Definitions", e);
        }

        YMetaData metaData = new YMetaData();
        metaData.setTitle(diagramShape.getProperty("spectitle"));
        metaData.setUniqueID(diagramShape.getProperty("specid") != null ? diagramShape.getProperty("specid") : "id" + UUID.randomUUID().toString());
        metaData.setDescription(diagramShape.getProperty("description"));

        try {
            metaData.setVersion(new YSpecVersion(diagramShape.getProperty("specversion")));
        } catch (Exception e) {
            getContext().addConversionWarnings("Could not convert metadata 'specversion' of specification", e);
            metaData.setVersion(new YSpecVersion());
        }

        try {
            if (diagramShape.getProperty("specvalidfrom") != null) {
                metaData.setValidFrom(new SimpleDateFormat(YAWLUtils.DATE_FORMAT).parse(diagramShape.getProperty("specvalidfrom")));
            }
        } catch (ParseException e) {
            getContext().addConversionWarnings("Invalid Date-Format specvalidfrom", e);
        }

        try {
            if (diagramShape.getProperty("specvaliduntil") != null) {
                metaData.setValidUntil(new SimpleDateFormat(YAWLUtils.DATE_FORMAT).parse(diagramShape.getProperty("specvaliduntil")));
            }
        } catch (ParseException e) {
            getContext().addConversionWarnings("Invalid Date-Format validTo", e);
        }

        try {
            if (diagramShape.getProperty("speccreated") != null) {
                metaData.setValidUntil(new SimpleDateFormat(YAWLUtils.DATE_FORMAT).parse(diagramShape.getProperty("speccreated")));
            }
        } catch (ParseException e) {
            getContext().addConversionWarnings("Invalid Date-Format validTo", e);
        }

        try {
            metaData.setCreators(convertListOfNames(diagramShape.getPropertyJsonObject("speccreators")));
        } catch (JSONException e) {
            getContext().addConversionWarnings("Could not convert metadata 'creators' of specification", e);
        }

        try {
            metaData.setContributors(convertListOfNames(diagramShape.getPropertyJsonObject("speccontributor")));
        } catch (JSONException e) {
            getContext().addConversionWarnings("Could not convert metadata 'contributor' of specification", e);
        }

        try {
            metaData.setSubjects(convertListOfNames(diagramShape.getPropertyJsonObject("specsubject")));
        } catch (JSONException e) {
            getContext().addConversionWarnings("Could not convert metadata 'subject' of specification", e);
        }

        metaData.setCoverage(diagramShape.getProperty("speccoverage"));
        metaData.setStatus(diagramShape.getProperty("specstatus"));
        metaData.setPersistent(diagramShape.getPropertyBoolean("specpersistent"));

        spec.setMetaData(metaData);
        return spec;
    }

    private List<String> convertListOfNames(JSONObject prop) throws JSONException {
        List<String> listOfNames = new ArrayList<String>();
        JSONArray jsonArray = prop.getJSONArray("items");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String name = obj.getString("name");
            listOfNames.add(name);
        }
        return listOfNames;
    }

}