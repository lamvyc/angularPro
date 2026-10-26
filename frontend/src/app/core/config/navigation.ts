import type { IconName } from '../../shared/ui/icon/icon';

export interface NavItem {
  readonly label: string;
  readonly icon: IconName;
  readonly route?: string;
  readonly badge?: string;
  readonly disabled?: boolean;
}

export interface NavSection {
  readonly title: string;
  readonly items: readonly NavItem[];
}

/**
 * 导航由权限驱动（M1 接入后端菜单后替换为动态数据）。
 * 当前阶段保留完整信息架构，未实现模块以里程碑标记并禁用。
 */
export const NAV_SECTIONS: readonly NavSection[] = [
  {
    title: '总览',
    items: [{ label: '工作台', icon: 'dashboard', route: '/dashboard' }],
  },
  {
    title: '临床业务',
    items: [
      { label: '预约挂号', icon: 'calendar', badge: 'M2', disabled: true },
      { label: '医生工作站', icon: 'clipboard', badge: 'M3', disabled: true },
      { label: '电子病历', icon: 'prescription', badge: 'M4', disabled: true },
      { label: '处方管理', icon: 'pulse', badge: 'M5', disabled: true },
    ],
  },
  {
    title: '药房',
    items: [{ label: '药房库存', icon: 'package', badge: 'M6', disabled: true }],
  },
  {
    title: '系统',
    items: [{ label: '权限管理', icon: 'lock', badge: 'M1', disabled: true }],
  },
];
