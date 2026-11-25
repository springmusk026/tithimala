package app.tithimala.feature.calendar

import android.annotation.SuppressLint
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.rememberReducedMotionEnabled
import app.tithimala.core.model.BsDate
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val MAX_ANIMATED_PAGE_JUMP = 2

@SuppressLint("ModifierParameter")
@Composable
fun CalendarRoute(
    onOpenDay: (BsDate) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
    sharedDayModifier: @Composable (BsDate) -> Modifier = { Modifier }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotionEnabled()
    val pagerState = rememberPagerState(
        initialPage = uiState.pageIndexFor(uiState.currentYearMonth),
        pageCount = { uiState.pageCount }
    )
    val currentState by rememberUpdatedState(uiState)
    val currentOnOpenDay by rememberUpdatedState(onOpenDay)
    val currentReducedMotion by rememberUpdatedState(reducedMotion)

    LaunchedEffect(viewModel, pagerState) {
        var scrollJob: Job? = null
        viewModel.effects.collect { effect ->
            when (effect) {
                is CalendarEffect.NavigateToDay -> currentOnOpenDay(effect.bsDate)
                is CalendarEffect.ScrollToMonth -> {
                    scrollJob?.cancel()
                    scrollJob = launch {
                        val targetPage = currentState.pageIndexFor(effect.yearMonth)
                        if (pagerState.currentPage != targetPage) {
                            val distance = abs(targetPage - pagerState.currentPage)
                            if (currentReducedMotion || distance > MAX_ANIMATED_PAGE_JUMP) {
                                pagerState.scrollToPage(targetPage)
                            } else {
                                pagerState.animateScrollToPage(
                                    page = targetPage,
                                    animationSpec = TithimalaMotion.gentleSpring(reducedMotion = false)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CalendarScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
        sharedDayModifier = sharedDayModifier,
        pagerState = pagerState
    )
}
