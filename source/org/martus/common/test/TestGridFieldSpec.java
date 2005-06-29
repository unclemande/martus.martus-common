/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2004, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/
package org.martus.common.test;

import org.martus.common.fieldspec.CustomDropDownFieldSpec;
import org.martus.common.fieldspec.DropDownFieldSpec;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.fieldspec.GridFieldSpec;
import org.martus.common.fieldspec.TestCustomDropDownFieldSpec;
import org.martus.common.fieldspec.GridFieldSpec.UnsupportedFieldTypeException;
import org.martus.util.TestCaseEnhanced;


public class TestGridFieldSpec extends TestCaseEnhanced
{

	public TestGridFieldSpec(String name)
	{
		super(name);
	}
	
	public void testGridXmlFieldSpecLoaderLegacy() throws Exception
	{
		FieldSpec.XmlFieldSpecLoader loader = new FieldSpec.XmlFieldSpecLoader();
		loader.parse(SAMPLE_GRID_FIELD_XML_LEGACY);
		GridFieldSpec spec = (GridFieldSpec)loader.getFieldSpec();
		assertEquals(2, spec.getColumnCount());
		assertContains(SAMPLE_GRID_HEADER_LABEL_1, spec.getAllColumnLabels());
		assertContains(SAMPLE_GRID_HEADER_LABEL_2, spec.getAllColumnLabels());
		assertEquals(SAMPLE_GRID_FIELD_XML, spec.toString());
	}
	
	public void testGridXmlFieldSpecLoaderNormal() throws Exception
	{
		FieldSpec.XmlFieldSpecLoader loader = new FieldSpec.XmlFieldSpecLoader();
		loader.parse(SAMPLE_GRID_FIELD_XML);
		GridFieldSpec spec = (GridFieldSpec)loader.getFieldSpec();
		assertEquals(2, spec.getColumnCount());
		assertContains(SAMPLE_GRID_HEADER_LABEL_1, spec.getAllColumnLabels());
		assertContains(SAMPLE_GRID_HEADER_LABEL_2, spec.getAllColumnLabels());
		assertEquals(SAMPLE_GRID_FIELD_XML, spec.toString());
	}

	public void testGridXmlFieldSpecLoaderDropdown() throws Exception
	{
		FieldSpec.XmlFieldSpecLoader loader = new FieldSpec.XmlFieldSpecLoader();
		loader.parse(SAMPLE_GRID_FIELD_XML_DROPDOWN);
		GridFieldSpec spec = (GridFieldSpec)loader.getFieldSpec();
		assertEquals(3, spec.getColumnCount());
		assertContains(SAMPLE_GRID_HEADER_LABEL_1, spec.getAllColumnLabels());
		assertContains(SAMPLE_GRID_HEADER_LABEL_2, spec.getAllColumnLabels());
		assertContains(TestCustomDropDownFieldSpec.SAMPLE_DROPDOWN_LABEL, spec.getAllColumnLabels());
		assertEquals(SAMPLE_GRID_FIELD_XML_DROPDOWN, spec.toString());
	}

	public void testAddColumn() throws Exception
	{
		GridFieldSpec spec = new GridFieldSpec();

		String labelStringColumn = "TYPE_NORMAL";
		FieldSpec stringSpec = new FieldSpec(labelStringColumn, FieldSpec.TYPE_NORMAL);
		spec.addColumn(stringSpec);
		assertEquals(labelStringColumn, spec.getColumnLabel(0));
		assertEquals(FieldSpec.TYPE_NORMAL, spec.getColumnType(0));
		assertEquals(labelStringColumn, spec.getFieldSpec(0).getLabel());

		FieldSpec.XmlFieldSpecLoader loader = new FieldSpec.XmlFieldSpecLoader();
		loader.parse(TestCustomDropDownFieldSpec.SAMPLE_DROPDOWN_FIELD_XML);
		DropDownFieldSpec dropdownSpecToAdd = (DropDownFieldSpec)loader.getFieldSpec();
		spec.addColumn(dropdownSpecToAdd);
		assertEquals(TestCustomDropDownFieldSpec.SAMPLE_DROPDOWN_LABEL, spec.getColumnLabel(1));
		assertEquals(FieldSpec.TYPE_DROPDOWN, spec.getColumnType(1));

		DropDownFieldSpec dropdownSpecRetrieved = (DropDownFieldSpec)spec.getFieldSpec(1);
		assertEquals(3, dropdownSpecRetrieved.getCount());
		assertEquals(CustomDropDownFieldSpec.EMPTY_FIRST_CHOICE, dropdownSpecRetrieved.getValue(0));
		assertEquals(TestCustomDropDownFieldSpec.SAMPLE_DROPDOWN_CHOICE1, dropdownSpecRetrieved.getValue(1));
		assertEquals(TestCustomDropDownFieldSpec.SAMPLE_DROPDOWN_CHOICE2, dropdownSpecRetrieved.getValue(2));


		String labelBooleanColumn = "TYPE_BOOLEAN";
		FieldSpec booleanSpec = new FieldSpec(labelBooleanColumn, FieldSpec.TYPE_BOOLEAN);
		spec.addColumn(booleanSpec);
		assertEquals(labelBooleanColumn, spec.getColumnLabel(2));
		assertEquals(FieldSpec.TYPE_BOOLEAN, spec.getColumnType(2));
		assertEquals(labelBooleanColumn, spec.getFieldSpec(2).getLabel());

		FieldSpec dateSpec = new FieldSpec("TYPE_DATE", FieldSpec.TYPE_DATE);
		try
		{
			spec.addColumn(dateSpec);
			fail("TYPE_DATE: Not yet implemented should have thrown exception");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}
		
		FieldSpec dateRangeSpec = new FieldSpec("TYPE_DATERANGE", FieldSpec.TYPE_DATERANGE);
		try
		{
			spec.addColumn(dateRangeSpec);
			fail("TYPE_DATERANGE: Not yet implemented should have thrown exception");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}

		FieldSpec langSpec = new FieldSpec("TYPE_LANGUAGE", FieldSpec.TYPE_LANGUAGE);
		try
		{
			spec.addColumn(langSpec);
			fail("TYPE_LANGUAGE: Not yet implemented should have thrown exception");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}

		FieldSpec multiLineSpec = new FieldSpec("TYPE_MULTILINE", FieldSpec.TYPE_MULTILINE);
		try
		{
			spec.addColumn(multiLineSpec);
			fail("TYPE_MULTILINE: Not yet implemented should have thrown exception");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}

		FieldSpec gridSpec = new FieldSpec("TYPE_GRID", FieldSpec.TYPE_GRID);
		try
		{
			spec.addColumn(gridSpec);
			fail("TYPE_GRID: Can NOT have a Grid inside of a Grid.");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}

		FieldSpec messageSpec = new FieldSpec("TYPE_MESSAGE", FieldSpec.TYPE_MESSAGE);
		try
		{
			spec.addColumn(messageSpec);
			fail("TYPE_MESSAGE: Can NOT have a Message inside of a Grid.");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}

		FieldSpec unknownSpec = new FieldSpec("TYPE_UNKNOWN", FieldSpec.TYPE_UNKNOWN);
		try
		{
			spec.addColumn(unknownSpec);
			fail("TYPE_UNKNOWN: Can NOT have an Unknown type inside of a Grid.");
		}
		catch(UnsupportedFieldTypeException expected)
		{
		}
	}

	public static final String SAMPLE_GRID_HEADER_LABEL_1 = "label1";
	public static final String SAMPLE_GRID_HEADER_LABEL_2 = "label2";
	public static final String SAMPLE_GRID_FIELD_XML_LEGACY = "<Field type='GRID'>\n" +
			"<Tag>custom</Tag>\n" +
			"<Label>me</Label>\n" +
			"<GridSpecDetails>\n<Column><Label>" +
			SAMPLE_GRID_HEADER_LABEL_1 +
			"</Label></Column>\n<Column><Label>" +
			SAMPLE_GRID_HEADER_LABEL_2 +
			"</Label></Column>\n</GridSpecDetails>\n</Field>\n";

	public static final String SAMPLE_GRID_FIELD_XML = "<Field type='GRID'>\n" +
	"<Tag>custom</Tag>\n" +
	"<Label>me</Label>\n" +
	"<GridSpecDetails>\n<Column type='STRING'>\n" +
	"<Tag></Tag>\n" +
	"<Label>"+SAMPLE_GRID_HEADER_LABEL_1+"</Label>\n" +
	"</Column>\n" +
	"<Column type='STRING'>\n" +
	"<Tag></Tag>\n" +
	"<Label>"+SAMPLE_GRID_HEADER_LABEL_2+"</Label>\n" +
	"</Column>\n" +
	"</GridSpecDetails>\n" +
	"</Field>\n";
	
	public static final String SAMPLE_DROPDOWN_CHOICE1 = "choice #1";
	public static final String SAMPLE_DROPDOWN_CHOICE2 = "choice #2";
	public static final String SAMPLE_DROPDOWN_LABEL = "Dropdown Label";
	public static final String SAMPLE_DROPDOWN_FIELD_XML = "<Column type='DROPDOWN'>\n" +
	"<Tag>custom</Tag>\n" +
	"<Label>"+SAMPLE_DROPDOWN_LABEL+"</Label>\n" +
	"<Choices>\n" +
	"<Choice>" +
	SAMPLE_DROPDOWN_CHOICE1 +
	"</Choice>\n" +
	"<Choice>" +
	SAMPLE_DROPDOWN_CHOICE2 +
	"</Choice>\n" +
	"</Choices>\n" +
	"</Column>\n";

	public static final String SAMPLE_GRID_FIELD_XML_DROPDOWN = "<Field type='GRID'>\n" +
	"<Tag>custom with dropdowns</Tag>\n" +
	"<Label>dropdowns</Label>\n" +
	"<GridSpecDetails>\n" +
	"<Column type='STRING'>\n" +
	"<Tag></Tag>\n" +
	"<Label>"+SAMPLE_GRID_HEADER_LABEL_1+"</Label>\n" +
	"</Column>\n" +
	"<Column type='STRING'>\n" +
	"<Tag></Tag>\n" +
	"<Label>"+SAMPLE_GRID_HEADER_LABEL_2+"</Label>\n" +
	"</Column>\n" +
	SAMPLE_DROPDOWN_FIELD_XML +
	"</GridSpecDetails>\n" +
	"</Field>\n";
	

}
