package com.leyton.backend.services.impl;

import com.leyton.backend.dto.CommitPerUserDTO;
import com.leyton.backend.dto.FilterRequest;
import com.leyton.backend.dto.StaticticByUnitDto;
import com.leyton.backend.entities.Application;
import com.leyton.backend.mappers.CommitMapper;
import com.leyton.backend.mappers.MemberMapper;
import com.leyton.backend.mappers.ProjectMapper;
import com.leyton.backend.repositories.CommitRepository;
import com.leyton.backend.repositories.MemberRepository;
import com.leyton.backend.repositories.ProjectRepository;
import com.leyton.backend.services.ApplicationService;
import com.leyton.backend.services.GitlabService;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GitlabServiceImpl implements GitlabService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabServiceImpl.class);

    @Autowired
    CommitRepository commitRepository;
    @Autowired
    ApplicationService applicationService;
    @Autowired
    CommitMapper commitMapper;
    GitLabApi gitLabApi;


    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ProjectMapper projectMapper;
    @Autowired
    MemberMapper memberMapper;

    @Override
    public List<Project> findAllProjects(int idProject) throws GitLabApiException {
        this.authentificationGitlab();

        Application application = applicationService.findApplication(Long.valueOf(idProject));
        //String path = application.getUrlGitlab().split("http://172.16.0.112/")[1];
        List<Project> projects = gitLabApi.getProjectApi().getOwnedProjects();
        if (projects.isEmpty()) {
            projects = Arrays.asList(gitLabApi.getProjectApi().getProject());
        }

        //this.startCron();
        //List<Project> list = gitLabApi.getProjectApi().getProjects();

        return projects;

    }

    @Override
    public List<Branch> findAllBranchsPerProjectPath(int idProject) throws GitLabApiException {
        Application application = applicationService.findApplication(Long.valueOf(idProject));
        String path = application.getUrlGitlab().split("http://172.16.0.112/")[1];
        this.authentificationGitlab();
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(path);
        return branches;
    }

    @Override
    public List<Branch> findAllBranchsPerProjectId(int idProject) throws GitLabApiException {
        this.authentificationGitlab();
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(idProject);
        return branches;
    }

    @Override
    public StaticticByUnitDto findAllCommitsPerProjectByUnit(FilterRequest filterRequest) throws ParseException, GitLabApiException {
        this.authentificationGitlab();

        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(filterRequest.getIdProject());

        List<Commit> commitArrayList = new ArrayList<>();
        for (Branch b : branches) {
            List<Commit> commitstemp = gitLabApi.getCommitsApi().getCommits(filterRequest.getIdProject(),
                    b.getName(), filterRequest.getDateFrom(), filterRequest.getDateTo());
            commitArrayList.addAll(commitstemp);
        }

        //  Map<String, List<Commit>> stringListMap =
        //        commits.stream().collect(Collectors.groupingBy(w -> w.getId()));

        //List<Commit> commits = gitLabApi.getCommitsApi().
        //      getCommits(filterRequest.getIdProject(),
        //            filterRequest.getIdBranch(), filterRequest.getDateFrom(), filterRequest.getDateTo());

        Set<String> nameSet = new HashSet<>();
        List<Commit> commits = commitArrayList.stream()
                .filter(e -> nameSet.add(e.getId()))
                .collect(Collectors.toList());

        Map<String, Map<Date, Integer>> map = new HashMap<>();
        String unit = "";

        if (!commits.isEmpty()) {
            Commit lastCommit = commits.get(0);
            Commit firstCommit = commits.get(commits.size() - 1);

            if (filterRequest.getDateFrom() == null) {
                String date = "01/01/1970";
                Date since = new SimpleDateFormat("dd/MM/yyyy").parse(date);
                filterRequest.setDateFrom(since);
            }

            if (filterRequest.getDateTo() == null) {
                filterRequest.setDateTo(new Date());
            }

            long diff = lastCommit.getCommittedDate().getTime() - firstCommit.getCommittedDate().getTime();
            long daysDiff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            LOGGER.info("Days diff : " + daysDiff);

            DateFormat dateFormat;

            if (30 > daysDiff) {
                dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                unit = "day";
            } else if (366 > daysDiff) {
                dateFormat = new SimpleDateFormat("MM-yyyy");
                unit = "month";
            } else {
                dateFormat = new SimpleDateFormat("yyyy");
                unit = "year";
            }

            Map<String, List<Commit>> commitPerUser =
                    commits.stream().collect(Collectors.groupingBy(w -> w.getCommitterEmail().split("@")[0].toLowerCase()));


            for (Map.Entry<String, List<Commit>> entry : commitPerUser.entrySet()) {
                User user = gitLabApi.getUserApi().getUser(entry.getKey());

                List<Commit> commitList = entry.getValue();

                Map<String, List<Commit>> stringListMap =
                        commitList.stream().collect(Collectors.groupingBy(w -> dateFormat.format(w.getCommittedDate())));

                Map<Date, Integer> dateIntegerMap =
                        stringListMap.entrySet().stream().collect(Collectors.toMap(
                                item -> {
                                    try {
                                        return dateFormat.parse(item.getKey());
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                },
                                item -> item.getValue().size()));

                if (user != null) {
                    map.put(user.getName(), new TreeMap<>(dateIntegerMap));
                } else {
                    map.put(entry.getValue().get(0).getCommitterName(), new TreeMap<>(dateIntegerMap));
                }

                //System.out.println("Key = " + entry.getKey() +
                //      ", Value = " + entry.getValue());
            }
        }


        StaticticByUnitDto staticticByUnitDto = new StaticticByUnitDto();
        staticticByUnitDto.setCommitPerUnit(map);
        staticticByUnitDto.setUnit(unit);

        return staticticByUnitDto;
    }

    @Override
    public CommitPerUserDTO findAllCommitsPerProject(FilterRequest filterRequest) throws GitLabApiException, ParseException {
        this.authentificationGitlab();

        //Application application = applicationService.findApplication(Long.valueOf(idProject));
        //String path = application.getUrlGitlab().split("http://172.16.0.112/")[1];

        if (filterRequest.getDateFrom() == null) {
            String sDate1 = "31/12/1998";
            Date since = new SimpleDateFormat("dd/MM/yyyy").parse(sDate1);
            filterRequest.setDateFrom(since);
        }

        if (filterRequest.getDateTo() == null) {
            filterRequest.setDateTo(new Date());
        }

        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(filterRequest.getIdProject());

        List<Commit> commitArrayList = new ArrayList<>();
        for (Branch b : branches) {
            List<Commit> commitstemp = gitLabApi.getCommitsApi().getCommits(filterRequest.getIdProject(),
                    b.getName(), filterRequest.getDateFrom(), filterRequest.getDateTo());
            commitArrayList.addAll(commitstemp);
        }
        Set<String> nameSet = new HashSet<>();
        List<Commit> commits = commitArrayList.stream()
                .filter(e -> nameSet.add(e.getId()))
                .collect(Collectors.toList());

        Map<String, List<Commit>> commitPerUser =
                commits.stream().collect(Collectors.groupingBy(w -> w.getCommitterEmail().split("@")[0].toLowerCase()));

        // Map<String, Integer> commitPerUserSize =
        //       commitPerUser.entrySet().stream().collect(Collectors.toMap(
        //             entry -> entry.getKey(),
        //           entry -> entry.getValue().size()));


        Map<String, Integer> commitPerUserSize = new HashMap<>();
        for (Map.Entry<String, List<Commit>> entry : commitPerUser.entrySet()) {
            User user = gitLabApi.getUserApi().getUser(entry.getKey());
            if (user != null) {
                commitPerUserSize.put(user.getName(), entry.getValue().size());
            } else {
                commitPerUserSize.put(entry.getValue().get(0).getCommitterName(), entry.getValue().size());
            }
        }

        CommitPerUserDTO commitPerUserDTO = new CommitPerUserDTO();
        commitPerUserDTO.setCommitPerUser(commitPerUserSize);
        commitPerUserDTO.setTotalCommit(commits.size());
        return commitPerUserDTO;
    }

    @Override
    public List<MergeRequest> findAllMergeRequestPerProject(int idProject) throws GitLabApiException {
        this.authentificationGitlab();

        List<MergeRequest> mergeRequests = gitLabApi.getMergeRequestApi().getMergeRequests(idProject);
        return mergeRequests;
    }


    public void startCron() throws GitLabApiException {

        List<Project> list = gitLabApi.getProjectApi().getProjects();

        /**
         List<Event> events = gitLabApi.getEventsApi().getUserEvents("fchablou",null,null,null,null, Constants.SortOrder.DESC);
         **/


        //saving Project list
        List<Project> projectList = gitLabApi.getProjectApi().getProjects();
        List<com.leyton.backend.entities.Project> toProjectDTOs = projectMapper.toProjectDTOs(projectList);

        for (int i = 0; i < toProjectDTOs.size(); i++) {
            try {
                projectRepository.save(toProjectDTOs.get(i));

            } catch (Exception e) {
                // LOGGER.error(e.get());
            }
        }
        //projectRepository.saveAll(toProjectDTOs);

        //saving memeber List
        List<User> userList = gitLabApi.getUserApi().getActiveUsers();
        List<com.leyton.backend.entities.Member> memberList = memberMapper.toMemberDTOs(userList);
        //memberRepository.saveAll(memberList);
        for (int i = 0; i < memberList.size(); i++) {
            try {
                memberRepository.save(memberList.get(0));
            } catch (Exception e) {
                // LOGGER.error(e.getMessage());
            }
        }

        List<com.leyton.backend.entities.Commit> commitArrayListDto = new ArrayList<>();
        for (int i = 0; i < projectList.size(); i++) {
            List<Commit> commits = new ArrayList<>();

            System.out.println("id Prpject" + projectList.get(i).getId());
            System.out.println("id Prpject" + projectList.get(i).getName());
            List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(projectList.get(i).getId());
            for (Branch b : branches) {
                System.out.println("id branch" + b.getName());
                try {
                    commits.addAll(gitLabApi.getCommitsApi().getCommits(projectList.get(i).getId(),
                            b.getName(), ""));
                } catch (Exception e) {

                }
            }
            List<com.leyton.backend.entities.Commit> commitList = new ArrayList<>();
            for (Commit commit : commits) {
                com.leyton.backend.entities.Commit commitDto = commitMapper.commitToCommitDTO(commit);
                commitDto.setIdProject(projectList.get(i).getId());
                commitList.add(commitDto);
            }
            commitArrayListDto.addAll(commitList);
            System.out.println(i);
        }


        Set<String> nameSet = new HashSet<>();
        List<com.leyton.backend.entities.Commit> commitList = commitArrayListDto.stream()
                .filter(e -> nameSet.add(e.getIdCommit()))
                .collect(Collectors.toList());


        // List<com.leyton.backend.entities.Commit> listt = commitMapper.toCommitDTOs(commitList);
        commitRepository.saveAll(commitList);


    }


    private Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        return cal.getTime();
    }

    void authentificationGitlab() throws GitLabApiException {
        gitLabApi = new GitLabApi("http://172.16.0.112/", "LosXScycd9Kx17WYskzB");
        gitLabApi.sudo("najebli");
    }
}
