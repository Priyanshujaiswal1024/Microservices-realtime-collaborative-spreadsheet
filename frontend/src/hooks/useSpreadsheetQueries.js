import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { workbookService } from '../services/workbookService';
import { commentService } from '../services/commentService';
import { auditService } from '../services/auditService';
import { notificationService } from '../services/notificationService';

// Query Keys
export const queryKeys = {
  workbooks: ['workbooks'],
  workbook: (id) => ['workbook', id],
  sheets: (workbookId) => ['sheets', workbookId],
  cells: (sheetId) => ['cells', sheetId],
  comments: (sheetId) => ['comments', sheetId],
  audit: (workbookId) => ['audit', workbookId],
  snapshots: (workbookId) => ['snapshots', workbookId],
  notifications: (userId) => ['notifications', userId],
};

/**
 * Hook to fetch single workbook details
 */
export function useWorkbookQuery(workbookId) {
  return useQuery({
    queryKey: queryKeys.workbook(workbookId),
    queryFn: () => workbookService.getWorkbook(workbookId),
    enabled: Boolean(workbookId),
    staleTime: 1000 * 60 * 5, // 5 mins
  });
}

/**
 * Hook to fetch all user workbooks
 */
export function useWorkbooksListQuery() {
  return useQuery({
    queryKey: queryKeys.workbooks,
    queryFn: () => workbookService.getUserWorkbooks(),
    staleTime: 1000 * 60 * 2,
  });
}

/**
 * Hook to fetch cell data for a sheet
 */
export function useSheetCellsQuery(sheetId) {
  return useQuery({
    queryKey: queryKeys.cells(sheetId),
    queryFn: () => workbookService.getSheetCells(sheetId),
    enabled: Boolean(sheetId),
    staleTime: 1000 * 30,
  });
}

/**
/**
 * Hook to fetch comment threads
 * Only fires when sheetId is a real server UUID (not a placeholder like 'sheet-1')
 */
const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function useCommentsQuery(sheetId) {
  return useQuery({
    queryKey: queryKeys.comments(sheetId),
    queryFn: () => commentService.getSheetComments(sheetId),
    enabled: Boolean(sheetId) && UUID_REGEX.test(sheetId), // Only real UUIDs
    staleTime: 1000 * 20,
    retry: false, // Don't retry on 404
  });
}


/**
 * Hook to fetch version history audit events
 */
export function useAuditHistoryQuery(workbookId) {
  return useQuery({
    queryKey: queryKeys.audit(workbookId),
    queryFn: () => auditService.getWorkbookHistory(workbookId),
    enabled: Boolean(workbookId),
  });
}

/**
 * Hook to fetch snapshots
 */
export function useSnapshotsQuery(workbookId) {
  return useQuery({
    queryKey: queryKeys.snapshots(workbookId),
    queryFn: () => auditService.getWorkbookSnapshots(workbookId),
    enabled: Boolean(workbookId),
  });
}

/**
 * Hook to fetch in-app notifications
 */
export function useNotificationsQuery(userId) {
  return useQuery({
    queryKey: queryKeys.notifications(userId),
    queryFn: () => notificationService.getUserNotifications(userId),
    enabled: Boolean(userId),
    refetchInterval: 30000,
  });
}

/**
 * Hook with all spreadsheet mutation operations
 */
export function useSpreadsheetMutations() {
  const queryClient = useQueryClient();

  const createWorkbookMutation = useMutation({
    mutationFn: (data) => workbookService.createWorkbook(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.workbooks });
    },
  });

  const createSheetMutation = useMutation({
    mutationFn: ({ workbookId, data }) => workbookService.createSheet(workbookId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.workbook(variables.workbookId) });
    },
  });

  const addCommentMutation = useMutation({
    mutationFn: ({ sheetId, data }) => commentService.createComment(sheetId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments(variables.sheetId) });
    },
  });

  const createSnapshotMutation = useMutation({
    mutationFn: ({ workbookId, data }) => auditService.createSnapshot(workbookId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.snapshots(variables.workbookId) });
    },
  });

  return {
    createWorkbookMutation,
    createSheetMutation,
    addCommentMutation,
    createSnapshotMutation,
  };
}
