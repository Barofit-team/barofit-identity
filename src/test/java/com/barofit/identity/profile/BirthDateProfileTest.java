package com.barofit.identity.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.barofit.common.error.BusinessException;
import com.barofit.identity.user.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BirthDateProfileTest {
  private static final UUID USER = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-23T15:30:00Z");

  private OnboardingCommand command(LocalDate date) {
    return new OnboardingCommand(
        "테스트",
        Gender.FEMALE,
        0,
        170,
        60,
        ExerciseGoal.HABIT,
        ExperienceLevel.NOVICE,
        3,
        Set.of(),
        CoachingStyle.FRIENDLY,
        date);
  }

  @Test
  void birthdayAndLeapDayUseCompletedYearsRatherThanCalendarYearDifference() {
    UserProfile profile = UserProfile.create(USER, command(LocalDate.of(2000, 11, 24)), 2026, NOW);
    assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2000, 11, 24));
    assertThat(profile.getBirthYear()).isEqualTo(2000);
    assertThat(profile.ageOn(LocalDate.of(2026, 11, 23))).isEqualTo(25);
    assertThat(profile.ageOn(LocalDate.of(2026, 11, 24))).isEqualTo(26);
    UserProfile leap = UserProfile.create(USER, command(LocalDate.of(2000, 2, 29)), 2026, NOW);
    assertThat(leap.ageOn(LocalDate.of(2026, 2, 28))).isEqualTo(25);
    assertThat(leap.ageOn(LocalDate.of(2026, 3, 1))).isEqualTo(26);
  }

  @Test
  void legacyAgeDoesNotInventABirthdayAndDateUpdateKeepsBirthYearConsistent() {
    UserProfile legacy =
        UserProfile.create(
            USER,
            new OnboardingCommand(
                "테스트",
                Gender.FEMALE,
                29,
                170,
                60,
                ExerciseGoal.HABIT,
                ExperienceLevel.NOVICE,
                3,
                Set.of(),
                CoachingStyle.FRIENDLY),
            2026,
            NOW);
    assertThat(legacy.getBirthDate()).isNull();
    assertThat(legacy.ageOn(LocalDate.of(2026, 11, 24))).isEqualTo(29);
    legacy.update(
        new ProfileUpdateCommand(
            null, null, null, null, null, null, null, null, null, null, LocalDate.of(2000, 11, 25)),
        2026,
        NOW);
    assertThat(legacy.getBirthYear()).isEqualTo(2000);
    assertThat(legacy.ageOn(LocalDate.of(2026, 11, 24))).isEqualTo(25);
    legacy.update(
        new ProfileUpdateCommand(null, null, 30, null, null, null, null, null, null, null),
        2026,
        NOW);
    assertThat(legacy.getBirthDate()).isEqualTo(LocalDate.of(2000, 11, 25));
  }

  @Test
  void serverValidatesAgeBoundariesUsingSeoulDate() {
    UserRepository users = mock(UserRepository.class);
    UserProfileRepository profiles = mock(UserProfileRepository.class);
    User user = mock(User.class);
    when(users.findByIdForUpdate(USER)).thenReturn(Optional.of(user));
    when(user.isActive()).thenReturn(true);
    when(profiles.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    ProfileService service =
        new ProfileService(
            users,
            profiles,
            mock(UserAccountService.class),
            mock(NicknameService.class),
            Clock.fixed(NOW, ZoneOffset.UTC));
    assertThat(service.onboard(USER, command(LocalDate.of(2012, 11, 24))).age()).contains(14);
    assertThat(service.onboard(USER, command(LocalDate.of(1925, 11, 25))).age()).contains(100);
    for (LocalDate date :
        List.of(LocalDate.of(2012, 11, 25), LocalDate.of(1925, 11, 24), LocalDate.of(2027, 1, 1))) {
      assertThatThrownBy(() -> service.onboard(USER, command(date)))
          .isInstanceOf(BusinessException.class);
    }
  }
}
