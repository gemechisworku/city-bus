package com.eegalepoint.citybus.workflow;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.workflow.WorkflowDefinitionEntity;
import com.eegalepoint.citybus.domain.workflow.WorkflowDefinitionRepository;
import com.eegalepoint.citybus.domain.workflow.WorkflowInstanceEntity;
import com.eegalepoint.citybus.domain.workflow.WorkflowInstanceRepository;
import com.eegalepoint.citybus.domain.workflow.WorkflowTaskDependencyEntity;
import com.eegalepoint.citybus.domain.workflow.WorkflowTaskDependencyRepository;
import com.eegalepoint.citybus.domain.workflow.WorkflowTaskEntity;
import com.eegalepoint.citybus.domain.workflow.WorkflowTaskRepository;
import com.eegalepoint.citybus.repo.UserRepository;
import com.eegalepoint.citybus.workflow.dto.BatchResultResponse;
import com.eegalepoint.citybus.workflow.dto.BatchTaskDecisionRequest;
import com.eegalepoint.citybus.workflow.dto.CreateTaskRequest;
import com.eegalepoint.citybus.workflow.dto.CreateWorkflowRequest;
import com.eegalepoint.citybus.workflow.dto.TaskResponse;
import com.eegalepoint.citybus.workflow.dto.WorkflowInstanceResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowService {

  public static final String TASK_PENDING = "PENDING";
  public static final String TASK_APPROVED = "APPROVED";
  public static final String TASK_REJECTED = "REJECTED";
  public static final String TASK_RETURNED = "RETURNED";

  public static final String WF_OPEN = "OPEN";
  public static final String WF_COMPLETED = "COMPLETED";
  public static final String WF_REJECTED = "REJECTED";

  private final WorkflowDefinitionRepository definitionRepository;
  private final WorkflowInstanceRepository instanceRepository;
  private final WorkflowTaskRepository taskRepository;
  private final WorkflowTaskDependencyRepository taskDependencyRepository;
  private final UserRepository userRepository;

  public WorkflowService(
      WorkflowDefinitionRepository definitionRepository,
      WorkflowInstanceRepository instanceRepository,
      WorkflowTaskRepository taskRepository,
      WorkflowTaskDependencyRepository taskDependencyRepository,
      UserRepository userRepository) {
    this.definitionRepository = definitionRepository;
    this.instanceRepository = instanceRepository;
    this.taskRepository = taskRepository;
    this.taskDependencyRepository = taskDependencyRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public WorkflowInstanceResponse createWorkflow(CreateWorkflowRequest req) {
    UserEntity user = currentUser();
    WorkflowDefinitionEntity def = definitionRepository.findById(req.definitionId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
    if (!def.isEnabled()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow definition is disabled");
    }
    WorkflowInstanceEntity instance = instanceRepository.save(
        new WorkflowInstanceEntity(def, req.title(), user));
    return toInstanceResponse(instance, List.of());
  }

  @Transactional(readOnly = true)
  public List<WorkflowInstanceResponse> listWorkflows(String status) {
    List<WorkflowInstanceEntity> instances = status != null
        ? instanceRepository.findByStatusOrderByCreatedAtDesc(status)
        : instanceRepository.findAllByOrderByCreatedAtDesc();
    return instances.stream()
        .map(i -> {
          List<WorkflowTaskEntity> tasks = taskRepository.findByInstance_IdOrderByCreatedAtAsc(i.getId());
          return toInstanceResponse(i, tasks);
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public WorkflowInstanceResponse getWorkflow(long instanceId) {
    WorkflowInstanceEntity instance = instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
    List<WorkflowTaskEntity> tasks = taskRepository.findByInstance_IdOrderByCreatedAtAsc(instanceId);
    return toInstanceResponse(instance, tasks);
  }

  @Transactional
  public TaskResponse createTask(CreateTaskRequest req) {
    WorkflowInstanceEntity instance = instanceRepository.findById(req.instanceId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
    UserEntity assignee = null;
    if (req.assignedToUserId() != null) {
      assignee = userRepository.findById(req.assignedToUserId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned user not found"));
    }
    LinkedHashSet<Long> predIds = new LinkedHashSet<>();
    if (req.predecessorTaskId() != null) {
      predIds.add(req.predecessorTaskId());
    }
    if (req.predecessorTaskIds() != null) {
      predIds.addAll(req.predecessorTaskIds());
    }
    for (Long predId : predIds) {
      WorkflowTaskEntity pred = taskRepository.findById(predId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Predecessor task not found"));
      if (!pred.getInstance().getId().equals(instance.getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Predecessor task must belong to the same workflow instance");
      }
    }
    WorkflowTaskEntity task = taskRepository.save(
        new WorkflowTaskEntity(instance, req.title(), req.description(), assignee));
    for (Long predId : predIds) {
      WorkflowTaskEntity pred = taskRepository.findById(predId).orElseThrow();
      taskDependencyRepository.save(new WorkflowTaskDependencyEntity(task, pred));
    }
    return toTaskResponse(task);
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> listTasks(String status) {
    List<WorkflowTaskEntity> tasks = status != null
        ? taskRepository.findByStatusOrderByCreatedAtDesc(status)
        : taskRepository.findAll();
    return tasks.stream().map(this::toTaskResponse).toList();
  }

  @Transactional
  public TaskResponse approveTask(long taskId, String note) {
    return decideTask(taskId, TASK_APPROVED, note);
  }

  @Transactional
  public TaskResponse rejectTask(long taskId, String note) {
    return decideTask(taskId, TASK_REJECTED, note);
  }

  @Transactional
  public TaskResponse returnTask(long taskId, String note) {
    return decideTask(taskId, TASK_RETURNED, note);
  }

  @Transactional
  public BatchResultResponse batchDecide(BatchTaskDecisionRequest req) {
    List<TaskResponse> results = new ArrayList<>();
    for (Long taskId : req.taskIds()) {
      String targetStatus = switch (req.action()) {
        case "APPROVE" -> TASK_APPROVED;
        case "REJECT" -> TASK_REJECTED;
        case "RETURN" -> TASK_RETURNED;
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action");
      };
      results.add(decideTask(taskId, targetStatus, req.note()));
    }
    return new BatchResultResponse(results.size(), results);
  }

  private TaskResponse decideTask(long taskId, String targetStatus, String note) {
    UserEntity user = currentUser();
    WorkflowTaskEntity task = taskRepository.findById(taskId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    if (!TASK_PENDING.equals(task.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already decided");
    }
    for (WorkflowTaskDependencyEntity dep : taskDependencyRepository.findByTask_IdOrderByDependsOn_IdAsc(taskId)) {
      WorkflowTaskEntity pred = dep.getDependsOn();
      if (!TASK_APPROVED.equals(pred.getStatus())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "All predecessor tasks must be approved first");
      }
    }
    task.setStatus(targetStatus);
    task.setDecidedBy(user);
    task.setDecisionNote(note);
    taskRepository.save(task);

    if (TASK_APPROVED.equals(targetStatus) || TASK_REJECTED.equals(targetStatus)) {
      recalculateInstanceStatus(task.getInstance());
    }

    return toTaskResponse(task);
  }

  private void recalculateInstanceStatus(WorkflowInstanceEntity instance) {
    List<WorkflowTaskEntity> tasks = taskRepository.findByInstance_IdOrderByCreatedAtAsc(instance.getId());
    if (tasks.isEmpty()) return;

    String approvalMode = instance.getDefinition().getApprovalMode();
    int requiredApprovals = instance.getDefinition().getRequiredApprovals();

    long approvedCount = tasks.stream().filter(t -> TASK_APPROVED.equals(t.getStatus())).count();
    boolean anyRejected = tasks.stream().anyMatch(t -> TASK_REJECTED.equals(t.getStatus()));
    boolean allDecided = tasks.stream().noneMatch(t -> TASK_PENDING.equals(t.getStatus()) || TASK_RETURNED.equals(t.getStatus()));

    switch (approvalMode) {
      case "ANY" -> {
        if (approvedCount >= 1) {
          instance.setStatus(WF_COMPLETED);
        } else if (allDecided) {
          instance.setStatus(WF_REJECTED);
        }
      }
      case "MAJORITY" -> {
        int majority = (tasks.size() / 2) + 1;
        int threshold = Math.max(majority, requiredApprovals);
        if (approvedCount >= threshold) {
          instance.setStatus(WF_COMPLETED);
        } else if (anyRejected && allDecided) {
          instance.setStatus(WF_REJECTED);
        }
      }
      default -> {
        if (anyRejected) {
          instance.setStatus(WF_REJECTED);
        } else if (allDecided) {
          instance.setStatus(WF_COMPLETED);
        }
      }
    }
    instanceRepository.save(instance);
  }

  private UserEntity currentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
  }

  private WorkflowInstanceResponse toInstanceResponse(WorkflowInstanceEntity i, List<WorkflowTaskEntity> tasks) {
    return new WorkflowInstanceResponse(
        i.getId(),
        i.getDefinition().getName(),
        i.getTitle(),
        i.getStatus(),
        i.getCreatedBy().getUsername(),
        i.getAssignedTo() != null ? i.getAssignedTo().getUsername() : null,
        tasks.stream().map(this::toTaskResponse).toList(),
        i.getCreatedAt(),
        i.getUpdatedAt());
  }

  private TaskResponse toTaskResponse(WorkflowTaskEntity t) {
    List<Long> preds =
        taskDependencyRepository.findByTask_IdOrderByDependsOn_IdAsc(t.getId()).stream()
            .map(d -> d.getDependsOn().getId())
            .toList();
    return new TaskResponse(
        t.getId(),
        t.getInstance().getId(),
        preds,
        t.getTitle(),
        t.getDescription(),
        t.getStatus(),
        t.getAssignedTo() != null ? t.getAssignedTo().getUsername() : null,
        t.getDecidedBy() != null ? t.getDecidedBy().getUsername() : null,
        t.getDecisionNote(),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
