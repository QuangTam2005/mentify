export const mentorKeys = {
  all: ['mentor'] as const,
  myApplication: () => [...mentorKeys.all, 'applications', 'me'] as const,
};
