package com.github.codergate.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.codergate.dto.RepositoryMinimal;
import com.github.codergate.dto.analysis.AnalysisDTO;
import com.github.codergate.dto.controller.RepositoryResponse;
import com.github.codergate.dto.installation.RepositoriesAddedDTO;
import com.github.codergate.entities.RepositoryEntity;
import com.github.codergate.entities.UserEntity;
import com.github.codergate.repositories.RepositoryRepository;

@Service
public class RepositoryService {

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AnalysisService analysisService;

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryService.class);

    /***
     * adds the repository to the table during installation event
     * 
     * @param repositories repository information
     * @param userId       user id
     * @return RepositoriesAdded dto class
     */
    public List<RepositoriesAddedDTO> addRepository(List<RepositoriesAddedDTO> repositories, int userId) {
        List<RepositoriesAddedDTO> repositoriesAddedDto = null;
        List<RepositoryEntity> repositoryEntityList = convertDTOToEntityForInstallationEvent(repositories, userId);
        if (repositoryEntityList != null) {
            List<RepositoryEntity> saveRepositoryEntity = repositoryRepository.saveAll(repositoryEntityList);
            LOGGER.info("addRepository :: saved repo info {}", saveRepositoryEntity);
            repositoriesAddedDto = convertEntityToDTO(saveRepositoryEntity);
        }
        return repositoriesAddedDto;
    }

    /***
     * adds the repository to the table during push event
     * 
     * @param
     * @param userId user id
     * @return RepositoryDTO
     */
    public RepositoryEntity addRepository(Integer id, String name, int userId, String installationId) {
        RepositoryEntity repositoryEntity = convertDTOToEntityForPushEvent(id, name, userId, installationId);
        RepositoryEntity saveRepositoryEntity = repositoryRepository.save(repositoryEntity);
        LOGGER.info("addRepository : The repositoryRepository information for push event is added {}",
                saveRepositoryEntity);
        return repositoryEntity;
    }

    /***
     * gets the repository information using id
     * 
     * @param repositoryId repositoryId
     * @return RepositoriesAdded dto class
     */
    public List<RepositoriesAddedDTO> getRepositoryById(List<Integer> repositoryId) {
        List<RepositoriesAddedDTO> repositoriesAddedDto = null;
        for (int id : repositoryId) {
            Optional<RepositoryEntity> repositoryEntity = repositoryRepository.findById(id);
            if (repositoryEntity.isPresent()) {
                List<RepositoryEntity> entity = new ArrayList<>();
                entity.add(repositoryEntity.get());
                repositoriesAddedDto = convertEntityToDTO(entity);
                LOGGER.info("getRepositoryById : Getting the repository information {}", entity);
            } else {
                LOGGER.warn("getRepositoryById: repository not found");
            }
        }
        return repositoriesAddedDto;
    }

    /***
     * updates the repository table
     * 
     * @param repositoryId repositoryId
     * @return RepositoriesAdded dto class
     */
    public List<RepositoriesAddedDTO> updateRepository(int repositoryId) {
        List<RepositoriesAddedDTO> repositoriesAddedDto = null;
        Optional<RepositoryEntity> repoOptional = repositoryRepository.findById(repositoryId);
        if (repoOptional.isPresent()) {
            List<RepositoryEntity> repoEntities = repoOptional.stream().map(repo -> {
                repo.setRepositoryId(repositoryId);
                return repo;
            }).collect(Collectors.toList());
            List<RepositoryEntity> saveEntity = repoEntities.stream()
                    .map(repo -> repositoryRepository.save(repo))
                    .collect(Collectors.toList());
            repositoriesAddedDto = convertEntityToDTO(saveEntity);
        }

        return repositoriesAddedDto;
    }

    /***
     * deletes the repository information
     * 
     * @param repositoryId repository id
     * @return true or false
     */
    public boolean deleteRepositoryById(int repositoryId) {
        boolean isDeleted = false;
        if (repositoryId != 0) {
            repositoryRepository.deleteById(repositoryId);
            isDeleted = true;
            LOGGER.info("deleteRepository : Deleting the repository information {}", repositoryId);
        }

        return isDeleted;
    }

    /***
     * converts dto to entity during installation event
     * 
     * @param repositoriesAddedDto dto class
     * @param userId               user id
     * @return RepositoryEntity
     */
    private List<RepositoryEntity> convertDTOToEntityForInstallationEvent(
            List<RepositoriesAddedDTO> repositoriesAddedDto, int userId) {

        List<RepositoryEntity> listOfRepositoryEntities = null;

        if (repositoriesAddedDto != null && userId != 0) {

            listOfRepositoryEntities = repositoriesAddedDto.stream().map(i -> {
                RepositoryEntity repositoryEntity = new RepositoryEntity();
                repositoryEntity.setRepositoryId(i.getId());
                repositoryEntity.setRepositoryName(i.getName());
                UserEntity userEntity = new UserEntity();
                userEntity.setUserId(userId);
                repositoryEntity.setUserEntity(userEntity);
                return repositoryEntity;
            }).collect(Collectors.toList());

            LOGGER.info("convertDTOToEntityForInstallationEvent : DTO has been converted to Entity {}",
                    listOfRepositoryEntities);
        } else {
            LOGGER.warn("convertDTOToEntityForInstallationEvent : Repository list or userId value is null");
        }
        return listOfRepositoryEntities;
    }

    /***
     * converts dto to entity during push event
     * 
     * @param
     * @param userID user id
     * @return RepositoryEntity
     */
    private RepositoryEntity convertDTOToEntityForPushEvent(Integer id, String name, int userID,
            String installationId) {
        RepositoryEntity repositoryEntity = null;
        if(id!=0 && name!=null && userID!=0 && installationId!=null) {
            repositoryEntity = new RepositoryEntity();
            repositoryEntity.setRepositoryId(id);
            repositoryEntity.setRepositoryName(name);
            UserEntity userEntity = new UserEntity();
            userEntity.setUserId(userID);
            repositoryEntity.setUserEntity(userEntity);
            repositoryEntity.setInstallationId(installationId);
            LOGGER.info("convertDTOToEntityForPushEvent : RepositoryRepository DTO has been converted to Entity {}",
                    repositoryEntity);
        } else {
            LOGGER.warn("convertDTOToEntityForInstallationEvent : Repository list or userId value is null");
        }
        return repositoryEntity;
    }

    /***
     * converts entity to dto during installation event
     * 
     * @param repositoryEntity RepositoryEntity class
     * @return dto class
     */
    private List<RepositoriesAddedDTO> convertEntityToDTO(List<RepositoryEntity> repositoryEntity) {
        List<RepositoriesAddedDTO> repoDtoList = null;
        if (repositoryEntity != null) {
            repoDtoList = repositoryEntity.stream()
                    .map(repo -> {
                        RepositoriesAddedDTO repositoryDto = new RepositoriesAddedDTO();
                        repositoryDto.setId(repo.getRepositoryId());
                        repositoryDto.setName(repo.getRepositoryName());
                        return repositoryDto;
                    }).collect(Collectors.toList());
            LOGGER.info("convertEntityToDTO : Entity has been converted to DTO {}", repoDtoList);
        } else {
            LOGGER.warn("convertEntityToDTO : Repository entity list is null");
        }
        return repoDtoList;
    }

    /***
     * gets the repository information using userId
     * 
     * @param userId userId
     * @return RepositoriesAdded dto class
     */
    public List<RepositoriesAddedDTO> getRepositoryFromUserId(Long userId) {
        List<RepositoriesAddedDTO> repositoriesAddedDto;
        List<RepositoryEntity> repositoriesByUserId = repositoryRepository.findByUserId(userId);
        LOGGER.info("getRepositoryFromUserId : Getting the repositories from user Id {}", repositoriesByUserId);
        repositoriesAddedDto = convertEntityToDTO(repositoriesByUserId);
        return repositoriesAddedDto;
    }

    public RepositoryResponse getRepositoryResponse(Long userId) {
        RepositoryResponse repositoryResponse = new RepositoryResponse();
        List<RepositoryEntity> repositoriesByUserId = repositoryRepository.findByUserId(userId);
        LOGGER.info("getRepositoryFromUserId : Getting the repositories from user Id {}", repositoriesByUserId);
        if (repositoriesByUserId != null) {
            List<RepositoryMinimal> reposAdded = repositoriesByUserId.stream()
                    .filter(Objects::nonNull)
                    .map(repo -> {
                        AnalysisDTO analysis = analysisService.getLatestAnalysis(repo.getRepositoryId());
                        String health;
                        double smellDensityCount = analysis.getCodeSmell();
                        if (smellDensityCount > 0 && smellDensityCount < 2) {
                            health = "A+";
                        } else if (smellDensityCount >= 2 && smellDensityCount < 4) {
                            health = "A";
                        } else if (smellDensityCount >= 4 && smellDensityCount < 5) {
                            health = "B+";
                        } else if (smellDensityCount >= 5 && smellDensityCount < 7) {
                            health = "B";
                        } else if (smellDensityCount >= 7 && smellDensityCount < 10) {
                            health = "F";
                        } else {
                            health = "-";
                        }
                        return new RepositoryMinimal(repo.getRepositoryId(), repo.getRepositoryName(),
                                analysis.getTimestamp(), health);
                    })
                    .collect(Collectors.toList());
            repositoryResponse.setRepositories(reposAdded);
        }
        return repositoryResponse;
    }

}
