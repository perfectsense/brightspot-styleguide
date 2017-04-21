/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Fuse from 'fuse.js'
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
      },
      filterOptions: {
        include: ['matches', 'score'],
        shouldSort: true,
        threshold: 0.3,
        location: 0,
        distance: 100,
        maxPatternLength: 32,
        minMatchCharLength: 1,
        keys: [{
          name: 'name',
          weight: 0.3
        },
        {
          name: 'groupName',
          weight: 1.0
        }]
      },
      highlightTag: 'b'
    }, options)
    this.searchResultsIndex = 0
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
    this.navList = []
    let $navigationLinks = $$(`.${this.selectors.groups}-examples-link`)
    // $navigationLinks.forEach((element) => {
    //   this.navList.push(element.text)
    // })
    let currentGroupName
    let exampleLinkIndex = 0
    $$(`.${this.selectors.groups}-name, .${this.selectors.groups}-examples-link`).forEach(($item, index, array) => {
      var item = {}
      if ($item.classList.value === 'StyleguideGroups-name') {
        currentGroupName = $item.childNodes[0].nodeValue
      } else {
        item.name = $item.childNodes[0].nodeValue
        item.groupName = currentGroupName
        item.index = exampleLinkIndex++
        this.navList.push(item)
      }
    })

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
    // binding events for search input
    this.ctx.querySelector(`.${this.selectors.dropdown}-search-input`).addEventListener('click', function (event) {
      if (!$(`.${self.selectors.dropdown}-results`)) {
        // create list container if it doesn't exist
        $.create('ul', {className: `${self.selectors.dropdown}-results`})._.after($(this))
      } else {
        // else removing the style display none property
        $(`.${self.selectors.dropdown}-results`).removeAttribute('style')
      }
    })

    let fuse = new Fuse(this.navList, this.settings.filterOptions)

    this.ctx.querySelector(`.${this.selectors.dropdown}-search-input`).addEventListener('keyup', function (event) {
      let $resultList = $(`.${self.selectors.dropdown}-results`)
      let $resultItems = $$(`.${self.selectors.dropdown}-result`)
      if (event.keyCode === 13 || event.keyCode === 32) {
        $resultList.querySelector('[data-active]').click()
        return false
      }

      if (event.keyCode === 40) {
        // down arrow
        self.stepThroughList($resultItems, 1)
        return false
      }

      if (event.keyCode === 38) {
        // up arrow
        self.stepThroughList($resultItems, -1)
        return false
      }

      // remove old list items

      while ($resultList.lastChild) {
        $resultList.removeChild($resultList.lastChild)
      }
      if (this.value === '') {
        return false
      }

      let results = fuse.search(this.value)
      console.log(results)
      let $searchInput = this
      this.searchResultsIndex = 0
      results.forEach((results, index, array) => {
        let resultName = results.item.name
        let resultGroupName = results.item.groupName

        // highlight matches for the name
        results.matches.forEach(function (item) {
          if (item.key === self.settings.filterOptions.keys[0].name) {
            resultName = self.highlightSearchKey(results.item.name, item.indices)
          }
          if (item.key === self.settings.filterOptions.keys[1].name) {
            resultGroupName = self.highlightSearchKey(results.item.groupName, item.indices)
          }
        })

        let listItemObj = $.create('li', {
          className: `${self.selectors.dropdown}-result`,
          innerHTML: `${resultName} <div class='${self.selectors.dropdown}-result-group'>${resultGroupName}</div>`,
          attributes: {
            'data-index': results.item.index,
            'data-score': results.score,
            'data-original': results.item.name
          },
          events: {
            click: function (evt) {
              // set the item to active
              this.parentNode.querySelector('[data-active]').removeAttribute('data-active')
              this.setAttribute('data-active', '')
              // set the searchResultsIndex when user clicks on item
              self.searchResultsIndex = index
              // trigger a click event on the navigational item
              $navigationLinks[results.item.index].click()
              // set the seearch input with the selected text
              $searchInput.value = results.item.name
              // hide the list
              $resultList.style.display = 'none'
              console.log(index)
            }
          }
        })
        // always set first item to active state
        if (index === 0) {
          listItemObj.setAttribute('data-active', '')
        }
        $resultList._.contents(listItemObj)
      })
    })

    // bind event for all section titles
    this.ctx.querySelectorAll(`.${this.selectors.groups}-name`).forEach(element => {
      element.addEventListener('click', function () {
        self.updateNavigationState(self.navigationState, this.childNodes[0].nodeValue)
        self.toggleMenu(this.parentNode)
      })
    })

    // bind event for dropdown filter section
    this.ctx.querySelector(`.${this.selectors.dropdown}-button`).addEventListener('click', function (event) {
      self.updateNavigationState(self.navigationState, this.childNodes[0].nodeValue)
      self.toggleMenu(this.parentNode)
    })
    // bind events for filter checkboxes
    this.ctx.querySelectorAll(`.${this.selectors.dropdown}-filter input[type=checkbox]`).forEach(function (element) {
      element.addEventListener('click', function (event) {
        self.filterList = Util.updateArray(self.filterList, this.value)
        let baseUrl = Util.updateSearchURL(window.location, {filters: self.filterList})
        window.history.pushState({}, `Filters:${self.filterList}`, baseUrl)
        self.addfilterAttributes(self.filterList)
      })
    })
  }
  // end of init method

  highlightSearchKey (text, matches) {
    let result = []
    let pair = matches.shift()
    // Build the formatted string
    for (var i = 0; i < text.length; i++) {
      var char = text.charAt(i)
      if (pair && i === pair[0]) {
        result.push(`<${this.settings.highlightTag}>`)
      }
      result.push(char)
      if (pair && i === pair[1]) {
        result.push(`</${this.settings.highlightTag}>`)
        pair = matches.shift()
      }
    }
    return result.join('')
  }

  stepThroughList ($resultItems, direction) {
    this.searchResultsIndex += direction
    if (this.searchResultsIndex >= $resultItems.length) {
      this.searchResultsIndex = 0
    }
    if (this.searchResultsIndex < 0) {
      this.searchResultsIndex = $resultItems.length - 1
    }
    // find the current active item and remove attribute
    $resultItems[this.searchResultsIndex].parentNode.querySelector('[data-active]').removeAttribute('data-active')
    // add active state to item
    $resultItems[this.searchResultsIndex].setAttribute('data-active', '')
  }

  toggleMenu ($ctxElement) {
    if ($ctxElement.getAttribute('data-selected') === '') {
      $ctxElement.removeAttribute('data-selected')
    } else {
      $ctxElement.setAttribute('data-selected', '')
    }
  }

  updateNavigationState (navigationArray, newValue) {
    navigationArray = Util.updateArray(navigationArray, newValue)
    window.localStorage.setItem('navigation-state', navigationArray)
  }
}
