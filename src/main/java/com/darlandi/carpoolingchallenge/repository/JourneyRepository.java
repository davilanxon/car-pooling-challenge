package com.darlandi.carpoolingchallenge.repository;

import com.darlandi.carpoolingchallenge.entities.Journey;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Journey Repository to store the journeys in DB.
 * Use HashOperations to save all the journeys in random order (very fast for reading)
 * Use ListOperations to keep the order of the journeys.
 */
@Repository
public class JourneyRepository {
    private static final String KEY = "JOURNEY";
    private static final String KEY_LIST = "WAITING-LIST";
    private final HashOperations hashOperations;
    private final ListOperations listOperations;

    public JourneyRepository(RedisTemplate redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
        this.listOperations = redisTemplate.opsForList();
    }

    /**
     * Save a new journey.
     * @param journey Journey object.
     */
    public void create(Journey journey) {
        hashOperations.putIfAbsent(KEY, journey.getId(), journey);
    }

    /**
     * Add a journey ID to the waiting list.
     * @param journeyId ID of the journey.
     */
    public void addToWaitingList(Long journeyId) {
        listOperations.rightPush(KEY_LIST, journeyId);
    }

    /**
     * Add a journey ID to the top of the list.
     * @param journeyId ID of the journey.
     */
    public void addTopWaitingList(Long journeyId) {
        listOperations.leftPush(KEY_LIST, journeyId);
    }

    /**
     * Get a Journey object with only its ID.
     * @param journeyId ID of the journey.
     * @return Optional Journey.
     */
    public Optional<Journey> get(Long journeyId) {
        try {
            Journey journey = (Journey) hashOperations.get(KEY, journeyId);
            return Optional.of(journey);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Get the first waiting journey.
     * @return Optional Journey.
     */
    public Optional<Journey> getFirstWaiting() {
        Long journeyId = (Long) listOperations.index(KEY_LIST, 0);
        return get(journeyId);
    }

    /**
     * Get all the IDs of waiting journeys.
     * @return List of IDs.
     */
    public List<Long> getAllWaitingIds() {
        List<Long> journeyList = new LinkedList<>();
        for (int i = 0; i < listOperations.size(KEY_LIST); i++) {
            journeyList.add((Long) listOperations.index(KEY_LIST, i));
        }
        return journeyList;
    }

    /**
     * Update an already saved journey.
     * @param journey object.
     */
    public void update(Journey journey) {
        hashOperations.put(KEY, journey.getId(), journey);
    }

    /**
     * Get the size of the waiting journey list.
     * @return Number of waiting journeys.
     */
    public Long sizeWaitingList(){
        return listOperations.size(KEY_LIST);
    }

    /**
     * Delete a journey.
     * @param journeyId ID of the journey.
     */
    public void delete(Long journeyId) {
        hashOperations.delete(KEY, journeyId);
    }


    /**
     * Remove a waiting journey ID from the waiting list.
     * @param journeyId ID of the journey.
     */
    public void removeWaitingList(Long journeyId) {
        for (int i = 0; i < listOperations.size(KEY_LIST); i++) {
            Long id = (Long) listOperations.index(KEY_LIST, i);
            if (id.equals(journeyId)) {
                listOperations.remove(KEY_LIST, i, journeyId);
                break;
            }
        }
    }

    /**
     * Clear all the journey and waiting journey lists.
     */
    public void deleteAll() {
        if (hashOperations.size(KEY) > 0) {
            Set<Long> journeyIdSet = hashOperations.keys(KEY);
            for (Long journeyId : journeyIdSet) {
                hashOperations.delete(KEY, journeyId);
            }
        }
        if (listOperations.size(KEY_LIST) > 0) {
            List<Long> journeyIdSet = listOperations.range(KEY_LIST, 0, listOperations.size(KEY_LIST));
            for (Long journeyId : journeyIdSet) {
                removeWaitingList(journeyId);
            }
        }
    }
}
