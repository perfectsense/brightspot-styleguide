/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Util from './util.js'
/* global $, $$ */

export class Navigation {
  get selectors () {
    return this.settings.selectors
  }

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {
        navigation: 'StyleguideNavigation',
        dropdown: 'StyleguideDropdown',
        groups: 'StyleguideGroups'
      }
    }, options)
  }

  addfilterAttributes (filtersArray) {
    let $navigationContent = $(`.${this.selectors.navigation}-content`)
    if (filtersArray.length > 0) {
      $navigationContent.setAttribute('data-filter', filtersArray.join(' '))
    } else {
      $navigationContent.removeAttribute('data-filter')
    }
  }

  init () {
    let self = this
    this.filterList = []
    this.navigationState = []
    let filtersStorage = Util.locationSearchToObject(window.location.search)

    if (filtersStorage['filters']) {
      this.filterList = filtersStorage['filters'].split(',')
      // update the checkboxes
      this.filterList.forEach(function (filerName) {
        let $inputFilter = $(`[name=filter-${filerName}]`)
        if ($inputFilter) {
          $inputFilter.checked = true
        }
      })
      // add filters to the navigation
      this.addfilterAttributes(this.filterList)
    }

    let navigationStorage = window.localStorage.getItem('navigation-state')
    if (navigationStorage === null) {
      navigationStorage = window.localStorage.setItem('navigation-state', '')
    }
    if (navigationStorage.length > 0) {
      this.navigationState = navigationStorage.split(',')
      // loop through dropdown and groupnames and set the state to selected if been toggled
      this.ctx.querySelectorAll(`.${this.selectors.dropdown}-button, .${this.selectors.groups}-name`).forEach((element) => {
        if (this.navigationState.indexOf(element.childNodes[0].nodeValue) > -1) {
          element.parentNode.setAttribute('data-selected', '')
        }
      })
    }

    this.ctx.querySelectorAll(`.${this.selectors.groups}-name`).forEach(element => {
      element.addEventListener('click', function () {
        let toggleElementAttr = this.parentNode
        self.navigationState = Util.updateArray(self.navigationState, this.childNodes[0].nodeValue)
        window.localStorage.setItem('navigation-state', self.navigationState)

        if (toggleElementAttr.getAttribute('data-selected') === '') {
          toggleElementAttr.removeAttribute('data-selected')
        } else {
          toggleElementAttr.setAttribute('data-selected', '')
        }
      })
    })

    this.ctx.querySelector(`.${this.selectors.dropdown}-button`).addEventListener('click', function (event) {
      let dropdownAttr = this.parentNode

      self.navigationState = Util.updateArray(self.navigationState, this.childNodes[0].nodeValue)
      window.localStorage.setItem('navigation-state', self.navigationState)

      if (dropdownAttr.getAttribute('data-selected') === '') {
        dropdownAttr.removeAttribute('data-selected')
      } else {
        dropdownAttr.setAttribute('data-selected', '')
      }
    })

    this.ctx.querySelectorAll(`.${this.selectors.dropdown}-filter input`).forEach(function (element) {
      element.addEventListener('click', function (event) {
        self.filterList = Util.updateArray(self.filterList, this.value)
        window.localStorage.setItem('navigation-filters', self.filterList)

        let baseUrl = Util.updateSearchURL(window.location, {filters: self.filterList})
        window.history.pushState({}, `Filters:${self.filterList}`, baseUrl)

        self.addfilterAttributes(self.filterList)
      })
    })
  }
}
