import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import {
  OpportunitiesDashboardView,
  getScoreBadgeClass,
} from './OpportunitiesDashboardView';
import type { OpportunityCardDto } from '../../types/api';

describe('OpportunitiesDashboardView - Cores de Compatibilidade', () => {
  describe('getScoreBadgeClass', () => {
    it('deve retornar verde (emerald) quando a compatibilidade for entre 70% e 100%', () => {
      expect(getScoreBadgeClass(100)).toContain('emerald');
      expect(getScoreBadgeClass(85)).toContain('emerald');
      expect(getScoreBadgeClass(70)).toContain('emerald');
    });

    it('deve retornar amarelo (amber) quando a compatibilidade for entre 50% e 69%', () => {
      expect(getScoreBadgeClass(69)).toContain('amber');
      expect(getScoreBadgeClass(60)).toContain('amber');
      expect(getScoreBadgeClass(50)).toContain('amber');
    });

    it('deve retornar vermelho (rose) quando a compatibilidade for menor que 50%', () => {
      expect(getScoreBadgeClass(49)).toContain('rose');
      expect(getScoreBadgeClass(30)).toContain('rose');
      expect(getScoreBadgeClass(0)).toContain('rose');
    });
  });

  describe('Renderização do bloco de compatibilidade', () => {
    const createMockOpportunity = (
      id: string,
      score: number
    ): OpportunityCardDto => ({
      job: {
        id,
        sourceId: null,
        externalId: `ext-${id}`,
        sourcePlatform: 'GUPY',
        companyName: 'Tech Corp',
        title: 'Desenvolvedor Frontend',
        descriptionRaw: 'Vaga de teste',
        mandatoryRequirements: ['React'],
        desirableRequirements: [],
        technologies: ['React', 'TypeScript'],
        seniority: 'Júnior',
        workModality: 'REMOTO',
        location: 'São Paulo, SP',
        salaryRange: 'R$ 5.000',
        benefits: [],
        applicationQuestions: [],
        originalUrl: 'https://example.com/job',
        publishedAt: '2026-11-05T10:00:00Z',
        active: true,
        discoveredAt: '2026-11-05T10:00:00Z',
      },
      matchAnalysis: {
        id: `match-${id}`,
        jobId: id,
        compatibilityScore: score,
        scoreBreakdown: {
          technologiesAndRequirementsScore: 40,
          seniorityAndRoleScore: 20,
          modalityAndLocationScore: 10,
          educationAndLanguagesScore: 5,
          desirableRequirementsBonus: 0,
        },
        positivePoints: ['Ótima aderência em React'],
        attentionPoints: [],
        matchedRequirements: ['React'],
        missingRequirements: [],
        aiSummary: `Score de ${score}% analisado pela IA.`,
        analyzedAt: '2026-11-05T10:00:00Z',
      },
      applicationId: null,
      applicationStatus: 'ENCONTRADA',
    });

    it('deve renderizar o bloco verde para vaga com 85% de compatibilidade', () => {
      const opp = createMockOpportunity('job-green', 85);
      render(
        <OpportunitiesDashboardView
          opportunities={[opp]}
          isMatchingAll={false}
          preparingJobId={null}
          onRunMatchAll={vi.fn()}
          onPrepareApplication={vi.fn()}
        />
      );

      const badge = screen.getByTestId('score-badge-job-green');
      expect(badge.className).toContain('emerald');
      expect(badge.textContent).toContain('85%');
      expect(badge.textContent).toContain('Compatibilidade');
    });

    it('deve renderizar o bloco amarelo para vaga com 65% de compatibilidade', () => {
      const opp = createMockOpportunity('job-yellow', 65);
      render(
        <OpportunitiesDashboardView
          opportunities={[opp]}
          isMatchingAll={false}
          preparingJobId={null}
          onRunMatchAll={vi.fn()}
          onPrepareApplication={vi.fn()}
        />
      );

      const badge = screen.getByTestId('score-badge-job-yellow');
      expect(badge.className).toContain('amber');
      expect(badge.textContent).toContain('65%');
    });

    it('deve renderizar o bloco vermelho para vaga com 45% de compatibilidade', () => {
      const opp = createMockOpportunity('job-red', 45);
      render(
        <OpportunitiesDashboardView
          opportunities={[opp]}
          isMatchingAll={false}
          preparingJobId={null}
          onRunMatchAll={vi.fn()}
          onPrepareApplication={vi.fn()}
        />
      );

      const badge = screen.getByTestId('score-badge-job-red');
      expect(badge.className).toContain('rose');
      expect(badge.textContent).toContain('45%');
    });
  });
});
