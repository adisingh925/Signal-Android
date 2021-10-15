package org.thoughtcrime.securesms.components.settings.app.subscription.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import org.thoughtcrime.securesms.components.settings.app.subscription.SubscriptionsRepository
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.util.livedata.Store

class ManageDonationsViewModel(
  private val subscriptionsRepository: SubscriptionsRepository
) : ViewModel() {

  private val store = Store(ManageDonationsState())
  private val eventPublisher = PublishSubject.create<ManageDonationsEvent>()
  private val disposables = CompositeDisposable()

  val state: LiveData<ManageDonationsState> = store.stateLiveData
  val events: Observable<ManageDonationsEvent> = eventPublisher

  init {
    store.update(Recipient.self().live().liveDataResolved) { self, state ->
      state.copy(featuredBadge = self.featuredBadge)
    }
  }

  override fun onCleared() {
    disposables.clear()
  }

  fun refresh() {
    disposables.clear()
    disposables += subscriptionsRepository.getActiveSubscription(SignalStore.donationsValues().getCurrency()).subscribeBy(
      onSuccess = { subscription -> store.update { it.copy(activeSubscription = subscription) } },
      onComplete = {
        store.update { it.copy(activeSubscription = null) }
        eventPublisher.onNext(ManageDonationsEvent.NOT_SUBSCRIBED)
      },
      onError = {
        eventPublisher.onNext(ManageDonationsEvent.ERROR_GETTING_SUBSCRIPTION)
      }
    )
  }

  class Factory(
    private val subscriptionsRepository: SubscriptionsRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(ManageDonationsViewModel(subscriptionsRepository))!!
    }
  }
}