import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Static blocks while the first answer is on its way. */
@Component({
  selector: 'mb-loading-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loading-skeleton.html',
  styleUrl: './loading-skeleton.css',
})
export class LoadingSkeleton {}
