import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/utils/queryKeys';
import {
  getScheduleList,
  getValidDataSetList,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  pauseSchedule,
  resumeSchedule,
  triggerSchedule,
  type ReportSchedule,
} from '@/services/reportSchedule';
import { getConfigList, type DeliveryConfig } from '@/services/deliveryConfig';

export interface ScheduleListParams {
  current: number;
  pageSize: number;
  datasetId?: number;
  enabled?: boolean;
}

export function useScheduleListQuery(params: ScheduleListParams) {
  return useQuery({
    queryKey: queryKeys.reportSchedule.list(params),
    queryFn: () => getScheduleList(params) as Promise<{ records: ReportSchedule[]; total: number }>,
    placeholderData: (prev) => prev,
  });
}

export function useValidDataSetMapQuery() {
  return useQuery({
    queryKey: queryKeys.dataSet.valid(),
    queryFn: async () => {
      const list = await getValidDataSetList();
      const arr = Array.isArray(list) ? list : (list as any)?.data ?? [];
      const map: Record<number, string> = {};
      arr.forEach((d: { id: number; name: string }) => { map[d.id] = d.name; });
      return map;
    },
    staleTime: 5 * 60_000,
  });
}

export function useDeliveryConfigMapQuery() {
  return useQuery({
    queryKey: queryKeys.deliveryConfig.list({ pageNum: 1, pageSize: 100 }),
    queryFn: async () => {
      const res = await getConfigList({ pageNum: 1, pageSize: 100 });
      const map: Record<number, DeliveryConfig> = {};
      ((res as any)?.records || []).forEach((c: DeliveryConfig) => { map[c.id] = c; });
      return map;
    },
    staleTime: 2 * 60_000,
  });
}

export function useScheduleSaveMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id?: number; values: Partial<ReportSchedule> }) =>
      v.id ? updateSchedule(v.id, v.values) : createSchedule(v.values),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() });
      qc.invalidateQueries({ queryKey: queryKeys.deliveryConfig.all() });
    },
  });
}

export function useScheduleDeleteMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteSchedule(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() }),
  });
}

export function useScheduleToggleMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; enabled: boolean }) =>
      v.enabled ? resumeSchedule(v.id) : pauseSchedule(v.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() }),
  });
}

export function useScheduleTriggerMutation() {
  return useMutation({
    mutationFn: (id: number) => triggerSchedule(id),
  });
}
