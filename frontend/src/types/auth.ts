/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

import { Expose } from 'class-transformer';
import { UserRole } from 'src/types/registration';

export class UserAuthenticationLoginResponse {
  @Expose()
  accessToken: string = '';

  @Expose()
  refreshToken: string = '';

  @Expose()
  username: string = '';

  @Expose()
  role: UserRole = UserRole.Guest;

  @Expose()
  authorities: string[] = [];

  @Expose()
  guestExpireSeconds: number = 0;

  @Expose()
  guestMaxProjects: number = 0;

  @Expose()
  guestMaxImages: number = 0;

  @Expose()
  guestMaxExpireSeconds: number = 0;

  toLimits() {
    const result = new GuestLimits();
    result.guestExpireSeconds = this.guestExpireSeconds;
    result.guestMaxExpireSeconds = this.guestMaxExpireSeconds;
    result.guestMaxProjects = this.guestMaxProjects;
    result.guestMaxImages = this.guestMaxImages;
    return result;
  }
}

export class GuestLimits {
  @Expose()
  guestExpireSeconds: number = 0;

  @Expose()
  guestMaxProjects: number = 0;

  @Expose()
  guestMaxImages: number = 0;

  @Expose()
  guestMaxExpireSeconds: number = 0;
}
