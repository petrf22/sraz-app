import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { map, Observable, tap } from "rxjs";
import { UserCreateGQL } from "../graphql/user-create.generated";
import { UserProfileGQL, UserProfileQuery } from "../graphql/user-profile.generated";
import { User } from "../graphql/graphql-types";

@Injectable({ providedIn: 'root' })
export class UserService {
  private api = inject(HttpClient);
  private userProfileGQL = inject(UserProfileGQL);
  private userCreateGQL = inject(UserCreateGQL);


  profile(): Observable<User | null> {
    return this.userProfileGQL.fetch().pipe(map(result => result.data?.userProfile ?? null));
  }

}