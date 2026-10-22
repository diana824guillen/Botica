import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button 
      [type]="type" 
      [disabled]="disabled" 
      [ngClass]="['btn', variant]"
      (click)="onClick.emit($event)">
      {{ label }}
    </button>
  `,
  styles: [`
    @use '../../../../styles/variables' as v;

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: v.$radius-md;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      font-size: 1rem;
      width: 100%;
    }
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .primary {
      background-color: v.$primary-color;
      color: white;
    }
    .primary:hover:not(:disabled) {
      background-color: v.$primary-hover;
    }
    .danger {
      background-color: v.$danger-color;
      color: white;
    }
    .secondary {
      background-color: v.$bg-surface;
      color: v.$text-main;
      border: 1px solid v.$border-color;
    }
  `]
})
export class ButtonComponent {
  @Input() label: string = 'Botón';
  @Input() type: 'button' | 'submit' = 'button';
  @Input() variant: 'primary' | 'secondary' | 'danger' = 'primary';
  @Input() disabled: boolean = false;
  @Output() onClick = new EventEmitter<Event>();
}


