package com.training.service;

import com.training.model.TrainingType;
import com.training.repository.TrainingTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingTypeService {

    private final TrainingTypeRepository trainingTypeRepository;

    public TrainingTypeService(TrainingTypeRepository trainingTypeRepository) {
        this.trainingTypeRepository = trainingTypeRepository;
    }

    @Transactional
    public TrainingType createTrainingType(TrainingType trainingType) {
        if (trainingTypeRepository.findByName(trainingType.getName()).isPresent()) {
            throw new RuntimeException("Направление с таким названием уже существует");
        }
        return trainingTypeRepository.save(trainingType);
    }

    @Transactional
    public TrainingType updateTrainingType(Long id, TrainingType trainingTypeDetails) {
        TrainingType trainingType = trainingTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));

        if (!trainingType.getName().equals(trainingTypeDetails.getName()) &&
                trainingTypeRepository.findByName(trainingTypeDetails.getName()).isPresent()) {
            throw new RuntimeException("Направление с таким названием уже существует");
        }

        trainingType.setName(trainingTypeDetails.getName());
        trainingType.setValidityPeriodMonths(trainingTypeDetails.getValidityPeriodMonths());
        trainingType.setDescription(trainingTypeDetails.getDescription());

        return trainingTypeRepository.save(trainingType);
    }

    @Transactional
    public void deleteTrainingType(Long id) {
        TrainingType trainingType = trainingTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));

        // Проверяем, есть ли связанные сертификаты
        if (!trainingType.getCertificates().isEmpty()) {
            throw new RuntimeException("Нельзя удалить направление с привязанными сертификатами");
        }

        trainingTypeRepository.delete(trainingType);
    }

    public List<TrainingType> getAllTrainingTypes() {
        return trainingTypeRepository.findAll();
    }

    public TrainingType getTrainingTypeById(Long id) {
        return trainingTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));
    }
}