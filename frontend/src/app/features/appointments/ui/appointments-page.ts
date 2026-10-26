import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AuthStore } from '../../../core/auth/auth.store';
import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { Icon } from '../../../shared/ui/icon/icon';
import type {
  AppointmentView,
  DoctorSchedule,
  Patient,
  ScheduleSlot,
} from '../data/appointment.models';
import { AppointmentStore } from '../state/appointment.store';
import { CancelDialog, PatientDialog } from './appointment-dialogs';

@Component({
  selector: 'app-appointments-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    HasPermissionDirective,
    Icon,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  templateUrl: './appointments-page.html',
  styleUrl: './appointments-page.scss',
})
export class AppointmentsPage {
  protected readonly store = inject(AppointmentStore);
  protected readonly auth = inject(AuthStore);
  private readonly dialog = inject(MatDialog);

  protected setBookingDate(event: Event): void {
    this.store.setBookingDate((event.target as HTMLInputElement).value);
  }

  protected setListDate(event: Event): void {
    this.store.setListDate((event.target as HTMLInputElement).value);
  }

  protected setStatus(event: Event): void {
    this.store.setStatusFilter((event.target as HTMLSelectElement).value);
  }

  protected onPatientKeyword(event: Event): void {
    this.store.patientKeyword.set((event.target as HTMLInputElement).value);
  }

  protected selectSchedule(schedule: DoctorSchedule): void {
    this.store.selectSchedule(schedule.id);
  }

  protected selectSlot(slot: ScheduleSlot): void {
    if (slot.available > 0) {
      this.store.selectSlot(slot.id);
    }
  }

  protected selectPatient(patient: Patient): void {
    this.store.selectPatient(patient.id);
  }

  protected openPatientDialog(): void {
    const ref = this.dialog.open<
      PatientDialog,
      unknown,
      Parameters<AppointmentStore['createPatient']>[0]
    >(PatientDialog, { width: '560px' });
    ref.afterClosed().subscribe((payload) => {
      if (payload) {
        this.store.createPatient(payload);
      }
    });
  }

  protected openCancelDialog(appointment: AppointmentView): void {
    const ref = this.dialog.open<CancelDialog, unknown, string>(CancelDialog, { width: '440px' });
    ref.afterClosed().subscribe((reason) => {
      if (reason) {
        this.store.cancel(appointment, reason);
      }
    });
  }

  protected statusLabel(status: AppointmentView['status']): string {
    switch (status) {
      case 'BOOKED':
        return '已预约';
      case 'CHECKED_IN':
        return '已签到';
      case 'IN_CONSULTATION':
        return '就诊中';
      case 'COMPLETED':
        return '已完成';
      case 'CANCELLED':
        return '已取消';
      default:
        return '过号';
    }
  }

  protected statusClass(status: AppointmentView['status']): string {
    switch (status) {
      case 'BOOKED':
        return 'tag tag-warning';
      case 'CHECKED_IN':
      case 'IN_CONSULTATION':
        return 'tag tag-success';
      case 'CANCELLED':
      case 'NO_SHOW':
        return 'tag tag-danger';
      default:
        return 'tag';
    }
  }

  protected chargeLabel(status: AppointmentView['chargeStatus']): string {
    switch (status) {
      case 'UNPAID':
        return '待收费';
      case 'PAID':
        return '已收费';
      case 'REFUNDED':
        return '已退款';
      case 'CANCELLED':
        return '账单取消';
      default:
        return '无账单';
    }
  }
}
