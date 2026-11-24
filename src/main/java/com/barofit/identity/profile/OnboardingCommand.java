package com.barofit.identity.profile;

import java.time.LocalDate;
import java.util.Set;

/** 온보딩 입력. 형식 검증은 web 요청 DTO, 닉네임 규칙은 ProfileService가 한다 */
public record OnboardingCommand(
    String nickname,
    Gender gender,
    int age,
    int heightCm,
    int weightKg,
    ExerciseGoal goal,
    ExperienceLevel experienceLevel,
    int weeklyWorkoutDays,
    Set<DiscomfortArea> discomfortAreas,
    CoachingStyle coachingStyle,
    LocalDate birthDate) {
  public OnboardingCommand(
      String nickname,
      Gender gender,
      int age,
      int heightCm,
      int weightKg,
      ExerciseGoal goal,
      ExperienceLevel experienceLevel,
      int weeklyWorkoutDays,
      Set<DiscomfortArea> discomfortAreas,
      CoachingStyle coachingStyle) {
    this(
        nickname,
        gender,
        age,
        heightCm,
        weightKg,
        goal,
        experienceLevel,
        weeklyWorkoutDays,
        discomfortAreas,
        coachingStyle,
        null);
  }
}
