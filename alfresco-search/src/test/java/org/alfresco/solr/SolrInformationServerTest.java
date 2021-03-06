/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.solr;

import java.io.IOException;
import java.util.Properties;

import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.content.SolrContentStore;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link SolrInformationServer} class.
 *
 * @author Matt Ward
 * @author Andrea Gazzarini
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrInformationServerTest
{
    private SolrInformationServer infoServer;

    @Mock
    private AlfrescoCoreAdminHandler adminHandler;

    @Mock
    private SolrResourceLoader resourceLoader;

    @Mock
    private SolrCore core;

    @Mock
    private SOLRAPIClient client;

    @Mock
    private SolrContentStore contentStore;

    @Mock
    private SolrRequestHandler handler;

    @Mock
    private SolrQueryResponse response;

    private SolrQueryRequest request;

    @Before
    public void setUp()
    {
        when(core.getResourceLoader()).thenReturn(resourceLoader);
        when(resourceLoader.getCoreProperties()).thenReturn(new Properties());
        infoServer = new SolrInformationServer(adminHandler, core, client, contentStore)
        {
            // @Override
            SolrQueryResponse newSolrQueryResponse()
            {
                return response;
            }
        };

        request = infoServer.newSolrQueryRequest();
    }

    @Test
    public void testGetStateOk()
    {
        String id = String.valueOf(System.currentTimeMillis());

        SolrDocument state = new SolrDocument();

        SimpleOrderedMap responseContent = new SimpleOrderedMap<>();
        responseContent.add(SolrInformationServer.RESPONSE_DEFAULT_ID, state);

        when(response.getValues()).thenReturn(responseContent);
        when(core.getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET)).thenReturn(handler);

        SolrDocument document = infoServer.getState(core, request, id);

        assertEquals(id, request.getParams().get(CommonParams.ID));
        verify(core).getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET);
        verify(response).getValues();

        assertSame(state, document);
    }

    /**
     * GetState returns null in case the given id doesn't correspond to an existing state document.
     */
    @Test
    public void testGetStateWithStateNotFound_returnsNull()
    {
        String id = String.valueOf(System.currentTimeMillis());

        SimpleOrderedMap responseContent = new SimpleOrderedMap<>();
        responseContent.add(SolrInformationServer.RESPONSE_DEFAULT_ID, null);

        when(response.getValues()).thenReturn(responseContent);
        when(core.getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET)).thenReturn(handler);

        SolrDocument document = infoServer.getState(core, request, id);

        assertEquals(id, request.getParams().get(CommonParams.ID));
        verify(core).getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET);
        verify(response).getValues();

        assertNull(document);
    }

    @Test
    public void testIndexAcl() throws IOException
    {
        /*
        // Source/expected data
        List<AclReaders> aclReadersList = new ArrayList<AclReaders>();
        aclReadersList.add(new AclReaders(101, Arrays.asList("r1", "r2", "r3"), Arrays.asList("d1", "d2"), 999,
                "example.com"));
        aclReadersList.add(new AclReaders(102, Arrays.asList("r4", "r5", "r6"), Arrays.asList("d3", "d4"), 999,
                "another.test"));
        aclReadersList.add(new AclReaders(103, Arrays.asList("GROUP_marketing", "simpleuser", "GROUP_EVERYONE",
                "ROLE_GUEST", "ROLE_ADMINISTRATOR", "ROLE_OWNER", "ROLE_RANDOM"), Arrays.asList(
                "GROUP_engineering", "justauser", "GROUP_EVERYONE", "ROLE_GUEST", "ROLE_ADMINISTRATOR",
                "ROLE_OWNER", "ROLE_RANDOM"), 999, "tenant.test"));
        aclReadersList.add(new AclReaders(104, Arrays.asList("GROUP_marketing", "simpleuser", "GROUP_EVERYONE",
                "ROLE_GUEST", "ROLE_ADMINISTRATOR", "ROLE_OWNER", "ROLE_RANDOM"), Arrays.asList(
                "GROUP_engineering", "justauser", "GROUP_EVERYONE", "ROLE_GUEST", "ROLE_ADMINISTRATOR",
                "ROLE_OWNER", "ROLE_RANDOM"), 999, "" Zero-length tenant == no mangling ));

        //final boolean willOverwrite = true;

        // Invoke the method under test
        //infoServer.indexAcl(aclReadersList, willOverwrite);


        // Capture the AddUpdateCommand instances for further analysis.
        //ArgumentCaptor<AddUpdateCommand> cmdArg = ArgumentCaptor.forClass(AddUpdateCommand.class);
        // The processor processes as many add commands as there are items in the aclReaderList.
        //verify(processor, times(aclReadersList.size())).processAdd(cmdArg.capture());

        // Verify that the AddUpdateCommand is as expected.
        //List<AddUpdateCommand> updates = cmdArg.getAllValues();
        //assertEquals("Wrong number of updates", aclReadersList.size(), updates.size());
        /*
        for (int docIndex = 0; docIndex < updates.size(); docIndex++)
        {
            AddUpdateCommand update = updates.get(docIndex);
            assertEquals("Overwrite flag was not correct value.", willOverwrite, update.overwrite);
            SolrInputDocument inputDoc = update.getSolrInputDocument();
            // Retrieve the original AclReaders object and compare with data in submitted SolrInputDocument
            final AclReaders sourceAclReaders = aclReadersList.get(docIndex);
            assertEquals(
                    AlfrescoSolrDataModel.getTenantId(sourceAclReaders.getTenantDomain()) + "!"
                            + NumericEncoder.encode(sourceAclReaders.getId()) + "!ACL",
                    inputDoc.getFieldValue("id").toString());
            assertEquals("0", inputDoc.getFieldValue("_version_").toString());
            assertEquals(sourceAclReaders.getId(), inputDoc.getFieldValue(QueryConstants.FIELD_ACLID));
            assertEquals(sourceAclReaders.getAclChangeSetId(), inputDoc.getFieldValue(QueryConstants.FIELD_INACLTXID));

            if (sourceAclReaders.getId() == 103)
            {
                // Authorities *may* (e.g. GROUP, EVERYONE, GUEST) be mangled to include tenant information
                final Collection<Object> docReaders = inputDoc.getFieldValues(QueryConstants.FIELD_READER);
                assertEquals(Arrays.asList("GROUP_marketing@tenant.test", "simpleuser", "GROUP_EVERYONE@tenant.test",
                        "ROLE_GUEST@tenant.test", "ROLE_ADMINISTRATOR", "ROLE_OWNER", "ROLE_RANDOM"), docReaders);
                final Collection<Object> docDenied = inputDoc.getFieldValues(QueryConstants.FIELD_DENIED);
                assertEquals(Arrays.asList("GROUP_engineering@tenant.test", "justauser", "GROUP_EVERYONE@tenant.test",
                        "ROLE_GUEST@tenant.test", "ROLE_ADMINISTRATOR", "ROLE_OWNER", "ROLE_RANDOM"), docDenied);
            }
            else
            {
                // Simple case, no authority/tenant mangling.
                assertEquals(sourceAclReaders.getReaders(), inputDoc.getFieldValues(QueryConstants.FIELD_READER));
                assertEquals(sourceAclReaders.getDenied(), inputDoc.getFieldValues(QueryConstants.FIELD_DENIED));
            }
        }
        */
    }
}
