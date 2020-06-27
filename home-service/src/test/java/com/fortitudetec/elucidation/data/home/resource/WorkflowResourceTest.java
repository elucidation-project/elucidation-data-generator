package com.fortitudetec.elucidation.data.home.resource;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.ElucidationResult;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.data.home.db.WorkflowDao;
import com.fortitudetec.elucidation.data.home.model.Workflow;
import com.fortitudetec.elucidation.data.home.service.WorkflowService;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("java:S100")
@DisplayName("WorkflowResource")
@ExtendWith(DropwizardExtensionsSupport.class)
class WorkflowResourceTest {

    private static final WorkflowDao WORKFLOW_DAO = mock(WorkflowDao.class);
    private static final ElucidationRecorder RECORDER = mock(ElucidationRecorder.class);
    private static final WorkflowService SERVICE = mock(WorkflowService.class);

    private static final DropwizardClientExtension RESOURCE
            = new DropwizardClientExtension(new WorkflowResource(WORKFLOW_DAO, SERVICE));
    private static final String NAME = "My First Workflow";

    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
        when(RECORDER.recordNewEvent(any(ConnectionEvent.class))).thenReturn(CompletableFuture.completedFuture(ElucidationResult.ok()));
    }

    @AfterEach
    void clearMocks() {
        reset(WORKFLOW_DAO);
    }

    @Nested
    class ListWorkflows {

        @Test
        void shouldReturnAListOfAllWorkflows() {
            var workflow = Workflow.builder()
                    .id(1L)
                    .name(NAME)
                    .stepJson("[]")
                    .build();

            when(WORKFLOW_DAO.findAll()).thenReturn(newArrayList(workflow));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/workflow")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var workflows = response.readEntity(new GenericType<List<Workflow>>(){});

            assertThat(workflows)
                    .hasSize(1)
                    .extracting("id", "name", "stepJson")
                    .contains(tuple(
                            workflow.getId(),
                            workflow.getName(),
                            workflow.getStepJson()));
        }

        @Test
        void shouldReturnEmptyListIfNoWorkflow() {
            when(WORKFLOW_DAO.findAll()).thenReturn(new ArrayList<>());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/workflow")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var workflows = response.readEntity(new GenericType<List<Workflow>>(){});

            assertThat(workflows).isEmpty();
        }
    }

    @Nested
    class CreateWorkflow {

        @Test
        void shouldReturn201_WithNewId() {
            var workflow = Workflow.builder()
                    .name(NAME)
                    .stepJson("[]")
                    .build();

            when(WORKFLOW_DAO.create(any(Workflow.class))).thenReturn(1L);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/workflow")
                    .request()
                    .post(json(workflow));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.readEntity(new GenericType<Map<String, Long>>(){}).get("id")).isEqualTo(1L);
        }
    }

    @Nested
    class DeleteWorkflow {

        @Test
        void shouldDeleteWorkflow() {
            when(WORKFLOW_DAO.deleteWorkflow(1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/workflow/{id}")
                    .resolveTemplate("id", 1L)
                    .request()
                    .delete();

            assertThat(response.getStatus()).isEqualTo(202);
            verify(WORKFLOW_DAO).deleteWorkflow(1L);
        }
    }
}
