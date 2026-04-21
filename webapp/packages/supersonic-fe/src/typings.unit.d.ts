// Type stubs used only during unit-test compilation (tsconfig.unit.json).
// These fill gaps left by @umijs/max globals that are unavailable in the
// standalone tsc invocation.

type Result<T = any> = T;

declare module '@umijs/max' {
  export const history: { location: { search: string }; push(...args: any[]): void; replace(...args: any[]): void };
  export function useModel(...args: any[]): any;
  export function request(...args: any[]): any;
}
