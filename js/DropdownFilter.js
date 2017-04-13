/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
/* global $, $$ */

export class DropdownFilter {
  get selectors () {
    return this.settings.selectors
  }

  get filterList () {
    return this._filterList
  }

  set filterList (list) {
    this._filterList = list
  }

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {

      }
    }, options)
  }
  addfilterAttributes (filtersArray) {
    if (filtersArray) {
      $('.StyleguideNavigation-content').setAttribute('data-filter', filtersArray.join(' '))
    }
  }

  updateFilterArray (value, remove) {
    let filterValue = value
    let matchIndex = this.filterList.indexOf(filterValue)
    if (matchIndex > -1) {
      if (!remove) {
        this.filterList.splice(matchIndex, 1)
      }
    } else {
      this.filterList.push(filterValue)
      console.log(this.filterList)
    }
  }

  init () {
    let self = this
    this.filterList = []
    var storedFilters = window.localStorage.getItem('navigation-filters')
    if (storedFilters === null) {
      storedFilters = window.localStorage.setItem('navigation-filters', '')
    }

    if (storedFilters.length > 0) {
      this.filterList = storedFilters.split(',')
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

    this.ctx.querySelector('.StyleguideDropdown-button').addEventListener('click', function (event) {
      let dropdownAttr = this.parentNode
      if (dropdownAttr.getAttribute('data-dropdown-open') === '') {
        dropdownAttr.removeAttribute('data-dropdown-open')
      } else {
        dropdownAttr.setAttribute('data-dropdown-open', '')
      }
    })

    this.ctx.querySelectorAll('.StyleguideDropdown-filter input').forEach(function (element) {
      element.addEventListener('click', function (event) {
        let removeValue = event.target.checked
        self.updateFilterArray(this.value, removeValue)
        window.localStorage.setItem('navigation-filters', self.filterList)
        self.addfilterAttributes(self.filterList)
      })
    })
  }
}
