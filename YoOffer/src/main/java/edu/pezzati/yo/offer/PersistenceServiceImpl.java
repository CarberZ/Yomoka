package edu.pezzati.yo.offer;

import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.bson.types.ObjectId;

import edu.pezzati.yo.offer.exception.OfferNotFound;
import edu.pezzati.yo.offer.model.Offer;

public class PersistenceServiceImpl implements PersistenceService {

	private Validator validator;
	private EntityManager entityM;
	private static PersistenceServiceImpl instance;

	private PersistenceServiceImpl(String persistenceUnitName) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
		EntityManagerFactory entityMFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
		entityM = entityMFactory.createEntityManager();
	}

	@Inject
	public static PersistenceService getInstance(String persistenceUnitName) {
		if (instance == null) {
			instance = new PersistenceServiceImpl(persistenceUnitName);
		}
		return instance;
	}

	@Override
	public Offer create(Offer offer) {
		evaluateValidation(validator.validate(offer));
		EntityTransaction transaction = entityM.getTransaction();
		transaction.begin();
		entityM.persist(offer);
		entityM.refresh(offer);
		transaction.commit();
		return offer;
	}

	@Override
	public Offer read(ObjectId id) throws OfferNotFound {
		Offer offer = entityM.find(Offer.class, id);
		if (offer == null) {
			throw new OfferNotFound();
		} else {
			return offer;
		}
	}

	@Override
	public Offer update(Offer offer) throws OfferNotFound {
		if (offer.getId() == null)
			throw new IllegalArgumentException("Invalid offer: " + offer.toString());
		evaluateValidation(validator.validate(offer));
		EntityTransaction transaction = entityM.getTransaction();
		transaction.begin();
		offer = entityM.find(Offer.class, offer.getId());
		if (offer == null)
			throw new OfferNotFound();
		entityM.merge(offer);
		entityM.refresh(offer);
		transaction.commit();
		return offer;
	}

	@Override
	public void dispose() {
		entityM.close();
		instance = null;
	}

	private void evaluateValidation(Set<ConstraintViolation<Offer>> validationResult) throws IllegalArgumentException {
		if (validationResult.isEmpty())
			return;
		StringBuilder errorMessage = new StringBuilder();
		for (ConstraintViolation<Offer> violation : validationResult) {
			errorMessage.append(violation.getMessage());
			errorMessage.append(", ");
		}
		throw new IllegalArgumentException(errorMessage.toString());
	}
}
