package vn.dentalcare.repository;

import vn.dentalcare.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleAndActiveTrue(Role role);
    List<User> findByRoleAndActiveTrueAndFullNameContainingIgnoreCase(Role role, String name);
    List<User> findByActiveTrue();
    long countByRoleAndActiveTrue(Role role);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);
}
