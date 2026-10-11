import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

describe('Design Tokens Foundation', () => {
  const globalsCssPath = path.resolve(__dirname, '../../app/globals.css');
  const globalsCss = fs.readFileSync(globalsCssPath, 'utf-8');

  it('defines light theme semantic CSS variables for Mentify Ocean Blue', () => {
    // Primary #2878BB
    expect(globalsCss).toContain('--primary: 207.3 64.8% 44.5%;');
    expect(globalsCss).toContain('--primary-foreground: 0 0% 100%;');

    // Secondary #8DCBF0
    expect(globalsCss).toContain('--secondary: 202.4 76.7% 74.7%;');

    // Deep Navy #102F4E / Canvas #F4FAFF
    expect(globalsCss).toContain('--foreground: 210 66% 18%;');
    expect(globalsCss).toContain('--background: 207 100% 98%;');

    // Radius 10px (8-12px range)
    expect(globalsCss).toContain('--radius: 10px;');
  });

  it('defines Ocean Blue 50-950 color scale under @theme', () => {
    expect(globalsCss).toContain('--color-primary-50: #f4f8fc;');
    expect(globalsCss).toContain('--color-primary-500: #2878bb;');
    expect(globalsCss).toContain('--color-primary-600: #22669f;');
    expect(globalsCss).toContain('--color-primary-950: #06121c;');
  });
});
